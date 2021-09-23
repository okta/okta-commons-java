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
package com.okta.commons.http.okhttp

import com.okta.commons.http.RequestExecutor
import com.okta.commons.http.config.HttpClientConfiguration
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.testng.annotations.Test

import java.util.concurrent.TimeUnit

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class OkHttpRequestExecutorFactoryTest {
    @Test
    void testDefaultConstructor() {
        def clientConfiguration = new HttpClientConfiguration()
        clientConfiguration.setConnectionTimeout(1111)
        def factory = new OkHttpRequestExecutorFactory()
        def executor = factory.create(clientConfiguration)
        def client = okHttpClientFromExecutor(executor)
        assertThat client.connectTimeoutMillis(), is(1111 * 1000)
        assertThat client.cookieJar(), is(CookieJar.NO_COOKIES)
    }

    @Test
    void testOkHttpClientConstructor() {
        def cookieJar = new CookieJar() {
            @Override
            void saveFromResponse(HttpUrl url, List<Cookie> cookies) {

            }

            @Override
            List<Cookie> loadForRequest(HttpUrl url) {
                return null
            }
        }
        def okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(12345, TimeUnit.MILLISECONDS)
            .callTimeout(12345, TimeUnit.MILLISECONDS)
            .cookieJar(cookieJar)
            .build()
        def clientConfiguration = new HttpClientConfiguration()
        clientConfiguration.setConnectionTimeout(1111)
        def factory = new OkHttpRequestExecutorFactory(okHttpClient)
        def executor = factory.create(clientConfiguration)
        def client = okHttpClientFromExecutor(executor)
        assertThat okHttpClient.connectTimeoutMillis(), is(12345)
        assertThat client.connectTimeoutMillis(), is(1111 * 1000)
        assertThat client.callTimeoutMillis(), is(12345)
        assertThat okHttpClient.cookieJar(), is(cookieJar)
        assertThat client.cookieJar(), is(CookieJar.NO_COOKIES)
        assertThat okHttpClient.retryOnConnectionFailure(), is(true)
        assertThat client.retryOnConnectionFailure(), is(false)
    }

    private static OkHttpClient okHttpClientFromExecutor(RequestExecutor executor) {
        return executor.delegate.client
    }
}
