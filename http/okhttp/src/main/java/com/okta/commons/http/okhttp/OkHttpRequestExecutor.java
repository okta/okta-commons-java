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

import com.okta.commons.http.DefaultResponse;
import com.okta.commons.http.HttpException;
import com.okta.commons.http.HttpHeaders;
import com.okta.commons.http.HttpMethod;
import com.okta.commons.http.MediaType;
import com.okta.commons.http.Request;
import com.okta.commons.http.RequestExecutor;
import com.okta.commons.http.RequestUtils;
import com.okta.commons.http.Response;
import com.okta.commons.http.authc.RequestAuthenticator;
import com.okta.commons.http.config.HttpClientConfiguration;
import com.okta.commons.http.config.Proxy;
import com.okta.commons.lang.Strings;
import okhttp3.CookieJar;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;

import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

/**
 */
public class OkHttpRequestExecutor implements RequestExecutor {

    private final OkHttpClient client;

    private final RequestAuthenticator requestAuthenticator;

    public OkHttpRequestExecutor(HttpClientConfiguration httpClientConfiguration) {
        this(httpClientConfiguration, createOkHttpClient(httpClientConfiguration));
    }

    OkHttpRequestExecutor(HttpClientConfiguration httpClientConfiguration, OkHttpClient okHttpClient) {
        this.requestAuthenticator = httpClientConfiguration.getRequestAuthenticator();

        if (httpClientConfiguration.getRequestExecutorParams().containsKey("debug")) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();

            switch (httpClientConfiguration.getRequestExecutorParams().get("debug").toUpperCase()){
                case "BODY":
                    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                    break;

                case "HEADERS":
                    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
                    break;

                default:
                    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
            }

            OkHttpClient.Builder clientBuilder = okHttpClient.newBuilder();
            clientBuilder.networkInterceptors().add(0, loggingInterceptor);
            this.client = clientBuilder.build();
        } else {
            this.client = okHttpClient;
        }
    }

    private static OkHttpClient createOkHttpClient(HttpClientConfiguration httpClientConfiguration) {

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        return configureOkHttpClient(httpClientConfiguration, clientBuilder);
    }

    static OkHttpClient configureOkHttpClient(HttpClientConfiguration httpClientConfiguration, OkHttpClient.Builder clientBuilder) {
        clientBuilder.connectTimeout(httpClientConfiguration.getConnectionTimeout(), TimeUnit.SECONDS);
        clientBuilder.readTimeout(httpClientConfiguration.getConnectionTimeout(), TimeUnit.SECONDS);
        clientBuilder.writeTimeout(httpClientConfiguration.getConnectionTimeout(), TimeUnit.SECONDS);

        clientBuilder.cookieJar(CookieJar.NO_COOKIES);
        clientBuilder.retryOnConnectionFailure(true); // fix for https://github.com/square/okhttp/issues/2738

        final Proxy sdkProxy = httpClientConfiguration.getProxy();
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
    public Response executeRequest(Request request) throws HttpException {

        // Sign the request
        this.requestAuthenticator.authenticate(request);

        HttpUrl.Builder urlBuilder = HttpUrl.get(request.getResourceUrl()).newBuilder();

        // query params
        request.getQueryString().forEach(urlBuilder::addQueryParameter);

        okhttp3.Request.Builder okRequestBuilder = new okhttp3.Request.Builder()
                             .url(urlBuilder.build());

        // headers
        request.getHeaders().toSingleValueMap().forEach(okRequestBuilder::addHeader);

        boolean isMultipartFormDataForFileUploading = false;
        String xContentType = RequestUtils.fetchHeaderValueAndRemoveIfPresent(request, "x-contentType");
        if(!Strings.isEmpty(xContentType)) {
            isMultipartFormDataForFileUploading = xContentType.equals(MediaType.MULTIPART_FORM_DATA_VALUE);
        }

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
                if(isMultipartFormDataForFileUploading) {
                    String fileLocation = RequestUtils.fetchHeaderValueAndRemoveIfPresent(request, "x-fileLocation");
                    String formDataPartName = RequestUtils.fetchHeaderValueAndRemoveIfPresent(request, "x-fileFormDataName");
                    File file = new File(fileLocation);
                    RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart(
                            formDataPartName,
                            file.getName(),
                            RequestBody.create(null, file)
                        )
                        .build();
                    okRequestBuilder.post(requestBody);
                } else {
                    okRequestBuilder.post(new InputStreamRequestBody(request.getBody(), request.getHeaders().getContentType()));
                }
                break;
            case PUT:
                // TODO support 100-continue ?
                okRequestBuilder.put(new InputStreamRequestBody(request.getBody(), request.getHeaders().getContentType()));
                break;
            default:
                throw new IllegalArgumentException("Unrecognized HttpMethod: " + method);
        }

        try {
            okhttp3.Response okResponse = client.newCall(okRequestBuilder.build()).execute();
            return toSdkResponse(okResponse);

        } catch (SocketException | SocketTimeoutException e) {
            throw new HttpException("Unable to execute HTTP request - retryable exception: " + e.getMessage(), e, true);
        } catch (IOException e) {
            throw new HttpException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("PMD.CloseResource")
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
            bodyInputStream = new ByteArrayInputStream(body.bytes());
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

        private final BufferedSource bufferedSource;

        private InputStreamRequestBody(InputStream inputStream, MediaType contentType) {
            this.inputStream = inputStream;
            this.okContentType = okhttp3.MediaType.parse(contentType.toString());
            if (inputStream == null) {
                this.bufferedSource = new Buffer();
            } else {
                this.bufferedSource = Okio.buffer(Okio.source(inputStream));
            }
        }

        @Override
        public okhttp3.MediaType contentType() {
            return okContentType;
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            try {
                sink.writeAll(bufferedSource.peek());
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (RuntimeException rethrown) {
                        throw rethrown;
                    } catch (Exception ignored) {
                        // Ignored, we errored from trying to close the stream.
                    }
                }
            }
        }

        @Override
        public long contentLength() throws IOException {
            return inputStream != null ? bufferedSource.peek().readByteArray().length : super.contentLength();
        }
    }
}
