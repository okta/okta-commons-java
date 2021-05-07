/*
 * Copyright 2014 Stormpath, Inc.
 * Modifications Copyright 2018-Present Okta, Inc.
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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.okta.commons.http.DefaultResponse
import com.okta.commons.http.HttpException
import com.okta.commons.http.HttpHeaders
import com.okta.commons.http.HttpMethod
import com.okta.commons.http.QueryString
import com.okta.commons.http.Request
import com.okta.commons.http.Response
import com.okta.commons.http.authc.RequestAuthenticator
import com.okta.commons.http.config.HttpClientConfiguration
import com.okta.commons.http.config.Proxy
import org.apache.http.Header
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.NoHttpResponseException
import org.apache.http.StatusLine
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.conn.ConnectTimeoutException
import org.slf4j.LoggerFactory
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeTest
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.stream.Collectors

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*
import static org.testng.Assert.assertNull

class HttpClientRequestExecutorTest {

    private Logger logger = (Logger) LoggerFactory.getLogger(HttpClientRequestExecutor)
    private ListAppender<ILoggingEvent> logAppender = new ListAppender<>()

    @BeforeClass
    void addTestLogAppender() {

        logAppender.start()
        logger.addAppender(logAppender)
    }

    @AfterClass
    void removeTestLogAppender() {
        logAppender.stop()
        logger.detachAppender(logAppender)
    }

    @Test //asserts https://github.com/stormpath/stormpath-sdk-java/issues/124
    void testToSdkResponseWithNullContentString() {

        def httpClientConfiguration = new HttpClientConfiguration()

        HttpResponse httpResponse = mock(HttpResponse)
        StatusLine statusLine = mock(StatusLine)
        HttpEntity entity = mock(HttpEntity)
        InputStream entityContent = mock(InputStream)

        when(httpResponse.getStatusLine()).thenReturn(statusLine)
        when(statusLine.getStatusCode()).thenReturn(200)
        when(httpResponse.getAllHeaders()).thenReturn(null)
        when(httpResponse.getEntity()).thenReturn(entity)
        when(entity.getContentEncoding()).thenReturn(null)
        when(entity.getContent()).thenReturn(entityContent)
        when(entity.getContentLength()).thenReturn(-1l)

        def e = new HttpClientRequestExecutor(httpClientConfiguration) {
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

        def requestAuthenticator = mock(RequestAuthenticator)

        def clientConfig = new HttpClientConfiguration()
        clientConfig.setProxy(new Proxy("example.com", 3333, "proxy-username", "proxy-password"))
        clientConfig.setRequestAuthenticator(requestAuthenticator)
        clientConfig.setConnectionTimeout(1111)
        clientConfig.setRetryMaxElapsed(2)
        clientConfig.setRetryMaxAttempts(3)

        def requestExecutor = new HttpClientRequestExecutor(clientConfig)

        assertThat requestExecutor.httpClient.connManager.pool.timeToLive, is(Duration.ofMinutes(5).toMillis())
        assertThat requestExecutor.httpClient.connManager.pool.validateAfterInactivity, is(Duration.ofSeconds(2).toMillis() as int)
    }

    @Test
    void testExecuteRequest() {

        def content = "my-content"
        def request = mockRequest()
        def requestAuthenticator = mock(RequestAuthenticator)
        def httpRequest = mock(HttpRequestBase)
        def httpClientRequestFactory = mock(HttpClientRequestFactory)
        def httpClient = mock(HttpClient)
        def httpResponse = mockHttpResponse(content)

        when(httpClientRequestFactory.createHttpClientRequest(request, null)).thenReturn(httpRequest)
        when(httpClient.execute(httpRequest)).thenReturn(httpResponse)

        def requestExecutor = createRequestExecutor(requestAuthenticator)
        requestExecutor.httpClientRequestFactory = httpClientRequestFactory
        requestExecutor.httpClient = httpClient

        def response = requestExecutor.executeRequest(request)
        def responseBody = response.body.text

        assertThat responseBody, is(content)
        assertThat response.httpStatus, is(200)
        assertThat response.isClientError(), is(false)
        assertThat response.isError(), is(false)
        assertThat response.isServerError(), is(false)

        verify(requestAuthenticator).authenticate(request)
        assertNoInfoOrHigherLogs()
    }

    @Test(dataProvider = "retryableExceptions")
    void throwRetryableExceptions(Exception e) {

        def request = mockRequest()
        def requestAuthenticator = mock(RequestAuthenticator)
        def httpRequest = mock(HttpRequestBase)
        def httpClientRequestFactory = mock(HttpClientRequestFactory)
        def httpClient = mock(HttpClient)

        when(httpClientRequestFactory.createHttpClientRequest(request, null)).thenReturn(httpRequest)
        when(httpClient.execute(httpRequest)).thenThrow(e)

        def requestExecutor = createRequestExecutor(requestAuthenticator)
        requestExecutor.httpClientRequestFactory = httpClientRequestFactory
        requestExecutor.httpClient = httpClient

        def restException = expect(HttpException, {requestExecutor.executeRequest(request)})
        assertThat "RestException.isRetryable expected to be true for: "+ e.getClass(), restException.isRetryable()

        verify(requestAuthenticator).authenticate(request)
    }

    @Test
    void throwIOExceptionNotRetryable() {

        def request = mockRequest()
        def requestAuthenticator = mock(RequestAuthenticator)
        def httpRequest = mock(HttpRequestBase)
        def httpClientRequestFactory = mock(HttpClientRequestFactory)
        def httpClient = mock(HttpClient)

        when(httpClientRequestFactory.createHttpClientRequest(request, null)).thenReturn(httpRequest)
        when(httpClient.execute(httpRequest)).thenThrow(new IOException("expected test IOException"))

        def requestExecutor = createRequestExecutor(requestAuthenticator)
        requestExecutor.httpClientRequestFactory = httpClientRequestFactory
        requestExecutor.httpClient = httpClient

        def restException = expect(HttpException, {requestExecutor.executeRequest(request)})
        assertThat "RestException.isRetryable expected to be false for: IOException", !restException.isRetryable()

        verify(requestAuthenticator).authenticate(request)
    }

    @DataProvider
    Object[][] retryableExceptions() {
        return [
                [new SocketException("expected test SocketException")],
                [new SocketTimeoutException("expected test SocketTimeoutException")],
                [new NoHttpResponseException("expected test NoHttpResponseException")],
                [new ConnectTimeoutException("expected test ConnectTimeoutException")]
        ]
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

        def clientConfig = mock(HttpClientConfiguration)

        when(clientConfig.getRequestAuthenticator()).thenReturn(requestAuthenticator)
        when(clientConfig.getConnectionTimeout()).thenReturn(1111)
        when(clientConfig.getRetryMaxElapsed()).thenReturn(maxElapsed)
        when(clientConfig.getRetryMaxAttempts()).thenReturn(maxAttempts)

        return clientConfig
    }

    private Request mockRequest(String uri = "https://example.com/a-resource",
                                HttpMethod method = HttpMethod.GET,
                                HttpHeaders headers = mock(HttpHeaders),
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

    void assertNoInfoOrHigherLogs() {
        String threadName = Thread.currentThread().getName()
        def logMessages = logAppender.list.stream()
            .filter( {it.threadName == threadName })
            .filter {it.level.isGreaterOrEqual(Level.INFO)}
            .collect(Collectors.toList())

        assertThat logMessages, empty()
    }
}