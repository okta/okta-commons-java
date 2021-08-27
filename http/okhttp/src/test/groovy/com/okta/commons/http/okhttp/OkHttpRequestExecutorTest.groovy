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
package com.okta.commons.http.okhttp

import com.okta.commons.http.HttpException
import com.okta.commons.http.HttpHeaders
import com.okta.commons.http.HttpMethod
import com.okta.commons.http.QueryString
import com.okta.commons.http.Request
import com.okta.commons.http.authc.DisabledAuthenticator
import com.okta.commons.http.authc.RequestAuthenticator
import com.okta.commons.http.config.HttpClientConfiguration
import com.okta.commons.http.config.Proxy
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody
import okio.Buffer
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

import static org.mockito.Mockito.*

class OkHttpRequestExecutorTest {

    @Test //asserts https://github.com/stormpath/stormpath-sdk-java/issues/124
    void testToSdkResponseWithNullContentString() {

        def requestAuthenticator = mock(RequestAuthenticator)
        
        def clientConfiguration = new HttpClientConfiguration()
        clientConfiguration.setRequestAuthenticator(requestAuthenticator)
        def requestExecutor = new OkHttpRequestExecutor(clientConfiguration)

        def okRequest = new okhttp3.Request.Builder()
                            .url("https://test.example.com")
                            .build()
        def okResponse = new okhttp3.Response.Builder()
                            .body(null)
                            .code(200)
                            .message("OK")
                            .request(okRequest)
                            .protocol(Protocol.HTTP_1_1)
                            .build()

        def response = requestExecutor.toSdkResponse(okResponse)

        assertThat response.body, nullValue()
        assertThat response.httpStatus, is(200)
    }

    @Test
    void testClientConfigurationConstructor() {

        def clientConfig = new HttpClientConfiguration()
        clientConfig.setProxy(new Proxy("example.com", 3333, "proxy-username", "proxy-password"))
        clientConfig.setRequestAuthenticator(new DisabledAuthenticator())
        clientConfig.setConnectionTimeout(1111)

        def requestExecutor = new OkHttpRequestExecutor(clientConfig)
        assertThat requestExecutor.client.proxy().type(), is(java.net.Proxy.Type.HTTP)
        assertThat requestExecutor.client.proxy().address().getHostName(), is("example.com")
        assertThat requestExecutor.client.proxy().address().getPort(), is(3333)
        assertThat requestExecutor.requestAuthenticator, instanceOf(DisabledAuthenticator)
        assertThat requestExecutor.client.connectTimeoutMillis(), is(1111 * 1000)
    }

    @Test
    void testExecuteRequest() {

        def content = "my-content"
        def okResponse = stubResponse(content)
        def headers = mock(HttpHeaders)
        def query = mock(QueryString)
        def request = mock(Request)
        def requestAuthenticator = mock(RequestAuthenticator)

        when(request.getHeaders()).thenReturn(headers)
        when(request.getResourceUrl()).thenReturn(new URI("https://testExecuteRequest.example.com"))
        when(request.getQueryString()).thenReturn(query)
        when(request.getMethod()).thenReturn(HttpMethod.GET)

        def interceptor = new Interceptor() {
            @Override
            okhttp3.Response intercept(Interceptor.Chain chain) throws IOException {
                return okResponse
            }
        }

        def requestExecutor = createRequestExecutor(new OkHttpClient.Builder()
                                                                    .addInterceptor(interceptor)
                                                                    .build(),
                                                    requestAuthenticator)

        def response = requestExecutor.executeRequest(request)
        def responseBody = response.body.text

        assertThat responseBody, is(content)
        assertThat response.httpStatus, is(200)
        assertThat response.isClientError(), is(false)
        assertThat response.isError(), is(false)
        assertThat response.isServerError(), is(false)

        verify(requestAuthenticator).authenticate(request)
    }

    @Test(dataProvider = "retryableExceptions")
    void throwRetryableExceptions(Exception e) {

        def headers = mock(HttpHeaders)
        def query = mock(QueryString)
        def request = mock(Request)
        def requestAuthenticator = mock(RequestAuthenticator)

        when(request.getHeaders()).thenReturn(headers)
        when(request.getResourceUrl()).thenReturn(new URI("https://testExecuteRequest.example.com"))
        when(request.getQueryString()).thenReturn(query)
        when(request.getMethod()).thenReturn(HttpMethod.GET)

        def interceptor = new Interceptor() {
            @Override
            okhttp3.Response intercept(Interceptor.Chain chain) throws IOException {
                throw e
            }
        }

        def requestExecutor = createRequestExecutor(new OkHttpClient.Builder()
                                                                    .addInterceptor(interceptor)
                                                                    .build(),
                                                    requestAuthenticator)

        def HttpException = expect(HttpException, {requestExecutor.executeRequest(request)})
        assertThat "HttpException.isRetryable expected to be true for: "+ e.getClass(), HttpException.isRetryable()

        verify(requestAuthenticator).authenticate(request)
    }

    @Test
    void throwIOExceptionNotRetryable() {

        def headers = mock(HttpHeaders)
        def query = mock(QueryString)
        def request = mock(Request)
        def requestAuthenticator = mock(RequestAuthenticator)

        when(request.getHeaders()).thenReturn(headers)
        when(request.getResourceUrl()).thenReturn(new URI("https://testExecuteRequest.example.com"))
        when(request.getQueryString()).thenReturn(query)
        when(request.getMethod()).thenReturn(HttpMethod.GET)

        def interceptor = new Interceptor() {
            @Override
            okhttp3.Response intercept(Interceptor.Chain chain) throws IOException {
                throw new IOException("expected test IOException")
            }
        }

        def requestExecutor = createRequestExecutor(new OkHttpClient.Builder()
                                                                    .addInterceptor(interceptor)
                                                                    .build(),
                                                    requestAuthenticator)

        def HttpException = expect(HttpException, {requestExecutor.executeRequest(request)})
        assertThat "HttpException.isRetryable expected to be false for: IOException", !HttpException.isRetryable()

        verify(requestAuthenticator).authenticate(request)
    }

    @Test
    void testInputStreamRequestBodyContentLength() {
        String content = "my-content"
        InputStream body = new ByteArrayInputStream(content.getBytes())
        def inputStreamRequestBody = new OkHttpRequestExecutor.InputStreamRequestBody(body, com.okta.commons.http.MediaType.TEXT_PLAIN)
        assertThat(inputStreamRequestBody.contentLength(), is((long) content.length()))
    }

    // https://github.com/okta/okta-oidc-android/issues/264
    @Test
    void testInputStreamRequestBodyCanBeReadTwice() {
        String content = "my-content"
        InputStream body = new ByteArrayInputStream(content.getBytes())
        def inputStreamRequestBody = new OkHttpRequestExecutor.InputStreamRequestBody(body, com.okta.commons.http.MediaType.TEXT_PLAIN)
        def buffer = new Buffer()

        assertThat(inputStreamRequestBody.contentLength(), is((long) content.length()))
        inputStreamRequestBody.writeTo(buffer)
        assertThat(buffer.readUtf8(), is(content))

        assertThat(inputStreamRequestBody.contentLength(), is((long) content.length()))
        inputStreamRequestBody.writeTo(buffer)
        assertThat(buffer.readUtf8(), is(content))
    }

    @Test
    void testNullInputStreamRequestBody() {
        def inputStreamRequestBody = new OkHttpRequestExecutor.InputStreamRequestBody(null, com.okta.commons.http.MediaType.TEXT_PLAIN)
        def buffer = new Buffer()

        assertThat(inputStreamRequestBody.contentLength(), is(-1L))
        inputStreamRequestBody.writeTo(buffer)
        assertThat(buffer.readUtf8(), is(emptyString()))
    }

    @DataProvider
    Object[][] retryableExceptions() {
        return [
            [new SocketException("expected test SocketException")],
            [new SocketTimeoutException("expected test SocketTimeoutException")]
        ]
    }

    def stubResponse(String body = "some-content", int code = 200, String url = "https://test.example.com") {

        def responseBody = ResponseBody.create(MediaType.parse("text/plain"), body)

        def okRequest = new okhttp3.Request.Builder()
                            .url(url)
                            .build()

        def okResponse = new okhttp3.Response.Builder()
                            .body(responseBody)
                            .code(code)
                            .message("OK")
                            .request(okRequest)
                            .protocol(Protocol.HTTP_1_1)
                            .build()

        return okResponse
    }

    def createRequestExecutor(OkHttpClient client = null, RequestAuthenticator requestAuthenticator = mock(RequestAuthenticator)) {


        def clientConfiguration = new HttpClientConfiguration()
        clientConfiguration.setRequestAuthenticator(requestAuthenticator)

        return client == null ? new OkHttpRequestExecutor(clientConfiguration)
                              : new OkHttpRequestExecutor(clientConfiguration, client)
    }

    static <T extends Throwable> T expect(Class<T> catchMe, Closure closure) {
        try {
            closure.call()
            Assert.fail("Expected ${catchMe.getName()} to be thrown.")
        } catch(e) {
            if (!e.class.isAssignableFrom(catchMe)) {
                throw e
            }
            return e
        }
    }
}