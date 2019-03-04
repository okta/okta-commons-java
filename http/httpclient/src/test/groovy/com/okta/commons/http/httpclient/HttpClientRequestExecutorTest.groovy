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
package com.okta.commons.http.httpclient

import com.okta.commons.http.Proxy
import com.okta.commons.http.ClientCredentials
import com.okta.commons.http.DefaultClientCredentialsResolver
import com.okta.commons.http.HttpClientConfiguration
import com.okta.commons.http.HttpHeaders
import com.okta.commons.http.HttpMethod
import com.okta.commons.http.QueryString
import com.okta.commons.http.Request
import com.okta.commons.http.Response
import com.okta.commons.http.AuthenticationScheme
import com.okta.commons.http.RequestAuthenticator
import com.okta.commons.http.RequestAuthenticatorFactory
import com.okta.commons.http.DefaultRequestAuthenticatorFactory
import com.okta.commons.http.DefaultResponse
import org.apache.http.Header
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.concurrent.FutureCallback
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.testng.Assert
import org.testng.annotations.Test

import java.nio.charset.StandardCharsets
import java.time.Duration

import static org.mockito.Mockito.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.testng.Assert.assertNull

class HttpClientRequestExecutorTest {

    @Test //asserts https://github.com/stormpath/stormpath-sdk-java/issues/124
    void testToSdkResponseWithNullContentString() {

        def clientCredentials = mock(ClientCredentials)

        HttpResponse httpResponse = mock(HttpResponse)
        StatusLine statusLine = mock(StatusLine)
        HttpEntity entity = mock(HttpEntity)
        InputStream entityContent = mock(InputStream)

        when(clientCredentials.getCredentials()).thenReturn("token-foo")
        when(httpResponse.getStatusLine()).thenReturn(statusLine)
        when(statusLine.getStatusCode()).thenReturn(200)
        when(httpResponse.getAllHeaders()).thenReturn(null)
        when(httpResponse.getEntity()).thenReturn(entity)
        when(entity.getContentEncoding()).thenReturn(null)
        when(entity.getContent()).thenReturn(entityContent)
        when(entity.getContentLength()).thenReturn(-1l)

        def clientConfig = new HttpClientConfiguration()
        clientConfig.setClientCredentialsResolver(new DefaultClientCredentialsResolver(clientCredentials))
        clientConfig.setProxy(new Proxy("example.com", 3333, "proxy-username", "proxy-password"))
        clientConfig.setConnectionTimeout(Duration.ofSeconds(1111L))
        clientConfig.setRetryMaxElapsed(2)
        clientConfig.setRetryMaxAttempts(3)

        def e = new HttpClientRequestExecutor(clientConfig) {
            @Override
            protected byte[] toBytes(HttpEntity he) throws IOException {
                return null
            }
        }

        def sdkResponse = e.toSdkResponse(httpResponse)

        assertNull sdkResponse.body
        assertThat sdkResponse.httpStatus, is(200)
    }

    @Test
    void testClientConfigurationConstructor() {

        def clientCredentials = mock(ClientCredentials)

        def clientConfig = new HttpClientConfiguration()
        clientConfig.setClientCredentialsResolver(new DefaultClientCredentialsResolver(clientCredentials))
        clientConfig.setProxy(new Proxy("example.com", 3333, "proxy-username", "proxy-password"))
        clientConfig.setConnectionTimeout(Duration.ofSeconds(1111L))
        clientConfig.setRetryMaxElapsed(2)
        clientConfig.setRetryMaxAttempts(3)

        def requestExecutor = new HttpClientRequestExecutor(clientConfig)

        assertThat requestExecutor.httpClientRequestFactory.defaultRequestConfig.connectTimeout, is(1111 * 1000)
    }

    @Test
    void testExecuteRequest() {

        def content = "my-content"
        def request = mockRequest()
        def requestAuthenticator = mock(RequestAuthenticator)
        def httpRequest = mock(HttpRequestBase)
        def httpClientRequestFactory = mock(HttpClientRequestFactory)
        def httpClient = mock(CloseableHttpAsyncClient)
        def httpResponse = mockHttpResponse(content)

        when(httpClientRequestFactory.createHttpClientRequest(request, null)).thenReturn(httpRequest)
        when(httpClient.execute(eq(httpRequest), any(FutureCallback))).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {

                FutureCallback callback = (FutureCallback) invocation.getArgument(1)
                callback.completed(httpResponse)
                return null
            }
        })

        def requestExecutor = createRequestExecutor(requestAuthenticator)
        requestExecutor.httpClientRequestFactory = httpClientRequestFactory
        requestExecutor.httpClient = httpClient

        def response = requestExecutor.executeRequestAsync(request).get()
        def responseBody = response.body.text

        assertThat responseBody, is(content)
        assertThat response.httpStatus, is(200)
        assertThat response.isClientError(), is(false)
        assertThat response.isError(), is(false)
        assertThat response.isServerError(), is(false)

        verify(requestAuthenticator).authenticate(request)
    }

    private static long time(Closure closure) {
        def startTime = System.currentTimeMillis()
        closure.call()
        return (System.currentTimeMillis() - startTime)
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

    private HttpClientRequestExecutor createRequestExecutor(RequestAuthenticator requestAuthenticator = mock(RequestAuthenticator), int maxElapsed = 15, int maxAttempts = 4) {

        return new HttpClientRequestExecutor(createClientConfiguration(requestAuthenticator, maxElapsed, maxAttempts))
    }

    private HttpClientConfiguration createClientConfiguration(RequestAuthenticator requestAuthenticator = mock(RequestAuthenticator), int maxElapsed = 15, int maxAttempts = 4) {

        def clientCredentials = mock(ClientCredentials)
        def clientConfig = mock(HttpClientConfiguration)

        when(clientConfig.getClientCredentialsResolver()).thenReturn(new DefaultClientCredentialsResolver(clientCredentials))
        when(clientConfig.getRequestAuthenticator()).thenReturn(requestAuthenticator)
        when(clientConfig.getConnectionTimeout()).thenReturn(Duration.ofSeconds(1111L))
        when(clientConfig.getRetryMaxElapsed()).thenReturn(maxElapsed)
        when(clientConfig.getRetryMaxAttempts()).thenReturn(maxAttempts)

        return clientConfig
    }

    private Request mockRequest(String uri = "https://example.com/a-resource",
                                HttpMethod method = HttpMethod.GET,
                                HttpHeaders headers = new HttpHeaders(),
                                QueryString queryString = new QueryString()) {

        def request = mock(Request)

        when(request.getQueryString()).thenReturn(queryString)
        when(request.getHeaders()).thenReturn(headers)
        when(request.getResourceUrl()).thenReturn(new URI(uri))
        when(request.getMethod()).thenReturn(method)

        return request
    }

    private Response stubResponse(String content, int statusCode = 200, Header[] headers = null) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8)
        return new DefaultResponse(statusCode, null, new ByteArrayInputStream(bytes), bytes.length)
    }

    private HttpResponse mockHttpResponse(String content, int statusCode = 200, Header[] headers = null) {

        def httpResponse = mock(HttpResponse)
        def statusLine = mock(StatusLine)
        def entity = mock(HttpEntity)
        def entityContent = new ByteArrayInputStream(content.getBytes())

        when(httpResponse.getStatusLine()).thenReturn(statusLine)
        when(httpResponse.getEntity()).thenReturn(entity)
        when(httpResponse.getAllHeaders()).thenReturn(headers)
        if (headers != null) {
            headers.each {
                when(httpResponse.getFirstHeader(it.name)).thenReturn(it)
                Header[] singleHeader = [it]
                when(httpResponse.getHeaders(it.name)).thenReturn(singleHeader)
            }
        }

        when(statusLine.getStatusCode()).thenReturn(statusCode)

        when(entity.getContentEncoding()).thenReturn(null)
        when(entity.getContent()).thenReturn(entityContent)
        when(entity.getContentLength()).thenReturn(content.length().longValue())

        return httpResponse
    }
}