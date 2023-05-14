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
package com.okta.commons.http

import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.emptyOrNullString
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class RequestUtilsTest {

    @Test
    void customHeadersInRequestTest() {

        def headers = new HttpHeaders()
        headers.add("x-header1", "value1")
        headers.add("x-header2", "value2")
        def content = "my-content"
        def request = mockRequest("https://example.com/a-resource", HttpMethod.GET, headers)

        def value1 = RequestUtils.fetchHeaderValueAndRemoveIfPresent(request, "x-header1")
        assertThat(value1, equalTo("value1"))

        def value2 = RequestUtils.fetchHeaderValueAndRemoveIfPresent(request, "x-header2")
        assertThat(value2, equalTo("value2"))

        def value3 = RequestUtils.fetchHeaderValueAndRemoveIfPresent(request, "x-header3")
        assertThat(value3, is(emptyOrNullString()))
    }

    @Test(dataProvider = "defaultPortData")
    void defaultPortTest(URI uri, boolean expected) {
        assertThat(RequestUtils.isDefaultPort(uri), is(expected))
    }

    @DataProvider
    Object[][] defaultPortData() {
        return [
            [new URI("https://example.com/a-resource"), true],
            [new URI("http://example.com:80/a-resource"), true],
            [new URI("https://example.com:80/a-resource"), false],
            [new URI("https://example.com:443/a-resource"), true],
            [new URI("http://example.com/a-resource"), true],
            [new URI("http://example.com:443/a-resource"), false],
            [new URI("http://example.com:8080/a-resource"), false],
            [new URI("https://example.com:8080/a-resource"), false]
        ]
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
}
