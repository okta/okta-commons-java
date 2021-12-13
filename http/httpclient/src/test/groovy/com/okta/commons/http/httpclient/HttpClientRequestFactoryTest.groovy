/*
 * Copyright 2021-Present Okta, Inc.
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

import com.okta.commons.http.HttpHeaders
import com.okta.commons.http.HttpMethod
import com.okta.commons.http.MediaType
import com.okta.commons.http.QueryString
import com.okta.commons.http.Request
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.entity.mime.MultipartFormEntity
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.instanceOf
import static org.hamcrest.Matchers.is
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class HttpClientRequestFactoryTest {

    @Test(dataProvider = "httpMethodDataProvider")
    void httpRequestTest(HttpMethod httpMethod) {

        String uri = "https://example.com/a-resource"
        def httpClientRequestFactory = new HttpClientRequestFactory(RequestConfig.custom().build())
        HttpHeaders headers = new HttpHeaders()
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        QueryString queryString = new QueryString()
        def request = mockRequest(uri, httpMethod, headers, queryString)

        HttpRequestBase httpRequest = httpClientRequestFactory.createHttpClientRequest(request, null)
        assertThat(httpRequest.getURI().toString(), is(uri))
        assertThat(httpRequest.getMethod(), equalTo(httpMethod.toString()))
    }

    @Test
    void postForMultipartFileUploadTest() {

        String uri = "https://example.com/a-resource"
        def httpClientRequestFactory = new HttpClientRequestFactory(RequestConfig.custom().build())
        HttpHeaders headers = new HttpHeaders()
        headers.add("x-contentType", MediaType.MULTIPART_FORM_DATA_VALUE)
        headers.add("x-fileLocation", "/path/to/file")
        headers.add("x-fileFormDataName", "file")
        QueryString queryString = new QueryString()
        def request = mockRequest(uri, HttpMethod.POST, headers, queryString)

        HttpRequestBase httpRequest = httpClientRequestFactory.createHttpClientRequest(request, null)
        assertThat(httpRequest.getURI().toString(), is(uri))
        assertThat(httpRequest.getMethod(), equalTo("POST"))
        assertThat((httpRequest as HttpPost).getEntity(), is(instanceOf(MultipartFormEntity)))
    }

    @DataProvider
    Object[] httpMethodDataProvider() {
        return [
            HttpMethod.POST,
            HttpMethod.GET,
            HttpMethod.PUT,
            HttpMethod.DELETE,
            HttpMethod.HEAD
        ]
    }

    private Request mockRequest(String uri, HttpMethod method, HttpHeaders headers, QueryString queryString) {

        def request = mock(Request)

        when(request.getQueryString()).thenReturn(queryString)
        when(request.getHeaders()).thenReturn(headers)
        when(request.getResourceUrl()).thenReturn(new URI(uri))
        when(request.getMethod()).thenReturn(method)
        when(request.getBody()).thenReturn(new ByteArrayInputStream(new byte[0]))

        return request
    }
}