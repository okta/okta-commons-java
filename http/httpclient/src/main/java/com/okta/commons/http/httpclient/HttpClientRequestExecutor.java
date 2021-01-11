/*
 * Copyright 2014 Stormpath, Inc.
 * Modifications Copyright 2018 Okta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okta.commons.http.httpclient;

import com.okta.commons.http.DefaultResponse;
import com.okta.commons.http.HttpException;
import com.okta.commons.http.HttpHeaders;
import com.okta.commons.http.MediaType;
import com.okta.commons.http.Request;
import com.okta.commons.http.RequestExecutor;
import com.okta.commons.http.Response;
import com.okta.commons.http.authc.RequestAuthenticator;
import com.okta.commons.http.config.HttpClientConfiguration;
import com.okta.commons.http.config.Proxy;
import com.okta.commons.lang.Assert;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

/**
 * {@code RequestExecutor} implementation that uses the
 * <a href="http://hc.apache.org/httpcomponents-client-ga">Apache HttpClient</a> implementation to
 * execute http requests.
 *
 * @since 0.5.0
 */
public class HttpClientRequestExecutor implements RequestExecutor {

    private static final Logger log = LoggerFactory.getLogger(HttpClientRequestExecutor.class);

    private final RequestAuthenticator requestAuthenticator;

    private HttpClient httpClient;

    private HttpClientRequestFactory httpClientRequestFactory;

    @SuppressWarnings({"deprecation"})
    public HttpClientRequestExecutor(HttpClientConfiguration clientConfiguration) {

        Proxy proxy = clientConfiguration.getProxy();
        Integer connectionTimeout = clientConfiguration.getConnectionTimeout();

        Assert.isTrue(connectionTimeout >= 0, "Timeout cannot be a negative number.");

        this.requestAuthenticator = clientConfiguration.getRequestAuthenticator();

        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(clientConfiguration.getConnectionTimeToLive(), TimeUnit.MILLISECONDS);
        connMgr.setValidateAfterInactivity(clientConfiguration.getConnectionValidationInactivity());

        if (clientConfiguration.getMaxConnectionTotal() >= clientConfiguration.getMaxConnectionPerRoute()) {
            connMgr.setDefaultMaxPerRoute(clientConfiguration.getMaxConnectionPerRoute());
            connMgr.setMaxTotal(clientConfiguration.getMaxConnectionTotal());
        } else {
            connMgr.setDefaultMaxPerRoute(clientConfiguration.getMaxConnectionPerRouteDefault());
            connMgr.setMaxTotal(clientConfiguration.getMaxConnectionTotalDefault());

            log.warn(
                "{} ({}) is less than {} ({}). " +
                "Reverting to defaults: connectionMaxTotal ({}) and connectionMaxPerRoute ({}).",
                clientConfiguration.getMaxConnectionsPerRoutePropertyKey(),
                clientConfiguration.getMaxConnectionTotal(),
                clientConfiguration.getMaxConnectionsTotalPropertyKey(),
                clientConfiguration.getMaxConnectionPerRoute(),
                clientConfiguration.getMaxConnectionTotalDefault(),
                clientConfiguration.getMaxConnectionPerRouteDefault()
            );
        }

        // The connectionTimeout value is specified in seconds in Okta configuration settings.
        // Therefore, multiply it by 1000 to be milliseconds since RequestConfig expects milliseconds.
        int connectionTimeoutAsMilliseconds = connectionTimeout * 1000;

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectionTimeoutAsMilliseconds)
                .setSocketTimeout(connectionTimeoutAsMilliseconds)
                .build();

        ConnectionConfig connectionConfig = ConnectionConfig.custom().setCharset(Consts.UTF_8).build();

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .disableCookieManagement()
                .setDefaultConnectionConfig(connectionConfig)
                .setConnectionManager(connMgr);

        this.httpClientRequestFactory = new HttpClientRequestFactory(requestConfig);

        if (proxy != null) {
            //We have some proxy setting to use!
            HttpHost httpProxyHost = new HttpHost(proxy.getHost(), proxy.getPort());
            httpClientBuilder.setProxy(httpProxyHost);

            if (proxy.isAuthenticationRequired()) {
                AuthScope authScope = new AuthScope(proxy.getHost(), proxy.getPort());
                Credentials credentials = new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword());
                CredentialsProvider credentialsProviderProvider = new BasicCredentialsProvider();
                credentialsProviderProvider.setCredentials(authScope, credentials);
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProviderProvider);
            }
        }

        httpClientBuilder.setRedirectStrategy(new LaxRedirectStrategy());

        this.httpClient = httpClientBuilder.build();
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Response executeRequest(Request request) throws HttpException {

        Assert.notNull(request, "Request argument cannot be null.");
        HttpResponse httpResponse = null;

        // Sign the request
        this.requestAuthenticator.authenticate(request);

        HttpRequestBase httpRequest = this.httpClientRequestFactory.createHttpClientRequest(request, null);

        try {
            httpResponse = httpClient.execute(httpRequest);
            return toSdkResponse(httpResponse);
        } catch (SocketException | SocketTimeoutException | NoHttpResponseException | ConnectTimeoutException e) {
            throw new HttpException("Unable to execute HTTP request - retryable exception: " + e.getMessage(), e, true);
        } catch (IOException e) {
            throw new HttpException("Unable to execute HTTP request: " + e.getMessage(), e);
        } finally {
            try {
                httpResponse.getEntity().getContent().close();
            } catch (Throwable ignored) { // NOPMD
            }
        }
    }

    protected byte[] toBytes(HttpEntity entity) throws IOException {
        return EntityUtils.toByteArray(entity);
    }

    protected Response toSdkResponse(HttpResponse httpResponse) throws IOException {

        int httpStatus = httpResponse.getStatusLine().getStatusCode();

        HttpHeaders headers = getHeaders(httpResponse);
        MediaType mediaType = headers.getContentType();

        HttpEntity entity = getHttpEntity(httpResponse);

        InputStream body = entity != null ? entity.getContent() : null;
        long contentLength;

        //ensure that the content has been fully acquired before closing the http stream
        if (body != null) {
            byte[] bytes = toBytes(entity);
            contentLength = entity.getContentLength();

            if(bytes != null) {
                body = new ByteArrayInputStream(bytes);
            }  else {
                body = null;
            }
        } else {
            contentLength = 0; // force 0 content length when there is no body
        }

        Response response = new DefaultResponse(httpStatus, mediaType, body, contentLength);
        response.getHeaders().putAll(headers);

        return response;
    }

    private HttpEntity getHttpEntity(HttpResponse response) {

        HttpEntity entity = response.getEntity();
        if (entity != null) {
            Header contentEncodingHeader = entity.getContentEncoding();
            if (contentEncodingHeader != null) {
                for (HeaderElement element : contentEncodingHeader.getElements()) {
                    if (element.getName().equalsIgnoreCase("gzip")) {
                        return new GzipDecompressingEntity(response.getEntity());
                    }
                }
            }
        }
        return entity;
    }

    private HttpHeaders getHeaders(HttpResponse response) {

        HttpHeaders headers = new HttpHeaders();
        Header[] httpHeaders = response.getAllHeaders();

        if (httpHeaders != null) {
            for (Header httpHeader : httpHeaders) {
                headers.add(httpHeader.getName(), httpHeader.getValue());
            }
        }

        return headers;
    }
}