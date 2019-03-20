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

import com.okta.commons.http.RequestExecutor;
import com.okta.commons.http.ClientCredentials;
import com.okta.commons.http.HttpClientConfiguration;
import com.okta.commons.http.HttpHeaders;
import com.okta.commons.http.Proxy;
import com.okta.commons.http.Request;
import com.okta.commons.http.Response;
import com.okta.commons.http.RestException;
import com.okta.commons.http.RequestAuthenticator;
import com.okta.commons.http.DefaultResponse;
import com.okta.commons.http.mime.MediaType;
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
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * {@code RequestExecutor} implementation that uses the
 * <a href="http://hc.apache.org/httpcomponents-client-ga">Apache HttpClient</a> implementation to
 * execute http requests.
 *
 * @since 0.5.0
 */
public class HttpClientRequestExecutor implements RequestExecutor {

    private static final Logger log = LoggerFactory.getLogger(HttpClientRequestExecutor.class);

    private static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = Integer.MAX_VALUE/2;
    private static final String MAX_CONNECTIONS_PER_ROUTE_PROPERTY_KEY = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.maxPerRoute";
    private static final int MAX_CONNECTIONS_PER_ROUTE;

    private static final int DEFAULT_MAX_CONNECTIONS_TOTAL = Integer.MAX_VALUE;
    private static final String MAX_CONNECTIONS_TOTAL_PROPERTY_KEY = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.maxTotal";
    private static final int MAX_CONNECTIONS_TOTAL;

    private final RequestAuthenticator requestAuthenticator;

    private CloseableHttpAsyncClient httpClient;

    private HttpClientRequestFactory httpClientRequestFactory;

    static {
        int connectionMaxPerRoute = DEFAULT_MAX_CONNECTIONS_PER_ROUTE;
        String connectionMaxPerRouteString = System.getProperty(MAX_CONNECTIONS_PER_ROUTE_PROPERTY_KEY);
        if (connectionMaxPerRouteString != null) {
            try {
                connectionMaxPerRoute = Integer.parseInt(connectionMaxPerRouteString);
            } catch (NumberFormatException nfe) {
                log.warn(
                    "Bad max connection per route value: {}. Using default: {}.",
                    connectionMaxPerRouteString, DEFAULT_MAX_CONNECTIONS_PER_ROUTE, nfe
                );
            }
        }
        MAX_CONNECTIONS_PER_ROUTE = connectionMaxPerRoute;

        int connectionMaxTotal = DEFAULT_MAX_CONNECTIONS_TOTAL;
        String connectionMaxTotalString = System.getProperty(MAX_CONNECTIONS_TOTAL_PROPERTY_KEY);
        if (connectionMaxTotalString != null) {
            try {
                connectionMaxTotal = Integer.parseInt(connectionMaxTotalString);
            } catch (NumberFormatException nfe) {
                log.warn(
                    "Bad max connection total value: {}. Using default: {}.",
                    connectionMaxTotalString, DEFAULT_MAX_CONNECTIONS_TOTAL, nfe
                );
            }
        }
        MAX_CONNECTIONS_TOTAL = connectionMaxTotal;
    }

    @SuppressWarnings({"deprecation"})
    public HttpClientRequestExecutor(HttpClientConfiguration clientConfiguration) {

        ClientCredentials clientCredentials = clientConfiguration.getClientCredentialsResolver().getClientCredentials();
        Proxy proxy = clientConfiguration.getProxy();
        long connectionTimeout = clientConfiguration.getConnectionTimeout().toMillis();

        Assert.notNull(clientCredentials, "clientCredentials argument is required.");
        Assert.isTrue(connectionTimeout >= 0, "Timeout cannot be a negative number.");

        this.requestAuthenticator = clientConfiguration.getRequestAuthenticator();

        int maxConnectionsTotal = MAX_CONNECTIONS_TOTAL;
        int maxConnectionsPerRoute = MAX_CONNECTIONS_PER_ROUTE;

        if (MAX_CONNECTIONS_TOTAL < MAX_CONNECTIONS_PER_ROUTE) {
            maxConnectionsTotal = DEFAULT_MAX_CONNECTIONS_PER_ROUTE;
            maxConnectionsPerRoute = DEFAULT_MAX_CONNECTIONS_TOTAL;

            log.warn(
                "{} ({}) is less than {} ({}). " +
                "Reverting to defaults: connectionMaxTotal ({}) and connectionMaxPerRoute ({}).",
                MAX_CONNECTIONS_TOTAL_PROPERTY_KEY, MAX_CONNECTIONS_TOTAL,
                MAX_CONNECTIONS_PER_ROUTE_PROPERTY_KEY, MAX_CONNECTIONS_PER_ROUTE,
                DEFAULT_MAX_CONNECTIONS_TOTAL, DEFAULT_MAX_CONNECTIONS_PER_ROUTE
            );
        }

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout((int)connectionTimeout)
                .setSocketTimeout((int)connectionTimeout)
                .build();

        ConnectionConfig connectionConfig = ConnectionConfig.custom().setCharset(Consts.UTF_8).build();

        HttpAsyncClientBuilder httpClientBuilder = HttpAsyncClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .disableCookieManagement()
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnPerRoute(maxConnectionsPerRoute)
                .setMaxConnTotal(maxConnectionsTotal);

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

    void setHttpClient(CloseableHttpAsyncClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public CompletableFuture<Response> executeRequestAsync(Request request, ExecutorService executorService) throws RestException {

        Assert.notNull(request, "Request argument cannot be null.");

        return CompletableFuture.supplyAsync(() -> {

            // Sign the request
            this.requestAuthenticator.authenticate(request);

            HttpRequestBase httpRequest = this.httpClientRequestFactory.createHttpClientRequest(request, null);

            CompletableFuture<Response> completableFuture = new CompletableFuture<>();

            httpClient.execute(httpRequest, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(HttpResponse httpResponse) {
                    try {
                        completableFuture.complete(toSdkResponse(httpResponse));
                    } catch (IOException e) {
                        failed(e);
                    } finally {
                        closeEntitySilently(httpResponse);
                    }
                }

                @Override
                public void failed(Exception e) {
                    boolean retryable = e instanceof NoHttpResponseException || e instanceof ConnectTimeoutException;
                    completableFuture.completeExceptionally(new RestException("Unable to execute HTTP request: " + e.getMessage(), e, retryable));
                }

                @Override
                public void cancelled() {
                    completableFuture.completeExceptionally(new InterruptedException("Http request was canceled"));
                }

                private void closeEntitySilently(HttpResponse httpResponse) {
                    try {
                        httpResponse.getEntity().getContent().close();
                    } catch (Throwable ignored) { // NOPMD
                    }
                }
            });

            return completableFuture;
        }, executorService).thenCompose(Function.identity());
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