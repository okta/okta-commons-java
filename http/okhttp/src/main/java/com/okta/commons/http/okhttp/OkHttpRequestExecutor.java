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
package com.okta.commons.http.okhttp;

import com.okta.commons.http.HttpClientConfiguration;
import com.okta.commons.http.HttpHeaders;
import com.okta.commons.http.HttpMethod;
import com.okta.commons.http.Proxy;
import com.okta.commons.http.Request;
import com.okta.commons.http.RequestExecutor;
import com.okta.commons.http.Response;
import com.okta.commons.http.RestException;
import com.okta.commons.http.RequestAuthenticator;
import com.okta.commons.http.DefaultResponse;
import com.okta.commons.http.mime.MediaType;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.CookieJar;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 */
public class OkHttpRequestExecutor implements RequestExecutor {

    private final OkHttpClient client;

    private final RequestAuthenticator requestAuthenticator;

    public OkHttpRequestExecutor(HttpClientConfiguration clientConfiguration) {
        this(clientConfiguration, createOkHttpClient(clientConfiguration));
    }

    // exposed for testing
    OkHttpRequestExecutor(HttpClientConfiguration clientConfiguration, OkHttpClient okHttpClient) {

        this.client = okHttpClient;
        this.requestAuthenticator = clientConfiguration.getRequestAuthenticator();
    }

    private static OkHttpClient createOkHttpClient(HttpClientConfiguration clientConfiguration) {

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        clientBuilder.connectTimeout(clientConfiguration.getConnectionTimeout().getSeconds(), TimeUnit.SECONDS);
        clientBuilder.readTimeout(clientConfiguration.getReadTimeout().getSeconds(), TimeUnit.SECONDS);
        clientBuilder.writeTimeout(clientConfiguration.getConnectionTimeout().getSeconds(), TimeUnit.SECONDS);

        clientBuilder.cookieJar(CookieJar.NO_COOKIES);
        clientBuilder.retryOnConnectionFailure(false); // handled by SDK

        final Proxy sdkProxy = clientConfiguration.getProxy();
        if (sdkProxy != null) {
            java.net.Proxy proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(sdkProxy.getHost(), sdkProxy.getPort()));

            clientBuilder.proxy(proxy);
            if (sdkProxy.isAuthenticationRequired()) {
                clientBuilder.proxyAuthenticator((route, response) -> {
                    String credential = Credentials.basic(sdkProxy.getUsername(), sdkProxy.getPassword());
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                });
            }
        }
        return clientBuilder.build();
    }

    @Override
    public CompletableFuture<Response> executeRequestAsync(Request request) throws RestException {

        // Sign the request
        this.requestAuthenticator.authenticate(request);

        HttpUrl.Builder urlBuilder = HttpUrl.get(request.getResourceUrl()).newBuilder();

        // query parms
        request.getQueryString().forEach(urlBuilder::addQueryParameter);

        okhttp3.Request.Builder okRequestBuilder = new okhttp3.Request.Builder()
                             .url(urlBuilder.build());

        // headers
        request.getHeaders().toSingleValueMap().forEach(okRequestBuilder::addHeader);

        HttpMethod method = request.getMethod();
        switch (method) {
            case DELETE:
                okRequestBuilder.delete();
                break;
            case GET:
                okRequestBuilder.get();
                break;
            case HEAD:
                okRequestBuilder.head();
                break;
            case POST:
                okRequestBuilder.post(new InputStreamRequestBody(request.getBody(), request.getHeaders().getContentType()));
                break;
            case PUT:
                // TODO support 100-continue ?
                okRequestBuilder.put(new InputStreamRequestBody(request.getBody(), request.getHeaders().getContentType()));
                break;
            default:
                throw new IllegalArgumentException("Unrecognized HttpMethod: " + method);
        }

        CompletableFuture<Response> completableFuture = new CompletableFuture<>();

        client.newCall(okRequestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                completableFuture.completeExceptionally(new RestException(e.getMessage(), e));
            }

            @Override
            public void onResponse(Call call, okhttp3.Response okResponse) throws IOException {
                completableFuture.complete(toSdkResponse(okResponse));
            }
        });
        return completableFuture;
    }

    private Response toSdkResponse(okhttp3.Response okResponse) throws IOException {

        int httpStatus = okResponse.code();

        HttpHeaders headers = new HttpHeaders();
        headers.putAll(okResponse.headers().toMultimap());

        MediaType mediaType = headers.getContentType();

        ResponseBody body = okResponse.body();
        InputStream bodyInputStream = null;
        long contentLength;

        //ensure that the content has been fully acquired before closing the http stream
        if (body != null) {
            contentLength = body.contentLength();
            byte[] bytes = body.bytes();

            if(bytes != null) {
                bodyInputStream = new ByteArrayInputStream(bytes);
            }
        } else {
            contentLength = 0; // force 0 content length when there is no body
        }

        Response response = new DefaultResponse(httpStatus, mediaType, bodyInputStream, contentLength);
        response.getHeaders().putAll(headers);

        return response;
    }

    private static class InputStreamRequestBody extends RequestBody {

        private final InputStream inputStream;

        private final okhttp3.MediaType okContentType;

        private InputStreamRequestBody(InputStream inputStream, MediaType contentType) {
            this.inputStream = inputStream;
            this.okContentType = okhttp3.MediaType.parse(contentType.toString());
        }

        @Override
        public okhttp3.MediaType contentType() {
            return okContentType;
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            Source source = null;
            try {
                source = Okio.source(inputStream);
                sink.writeAll(source);
            } finally {
                Util.closeQuietly(source);
            }
        }
    }
}