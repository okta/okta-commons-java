/*
 * Copyright 2018-Present Okta, Inc.
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

import com.google.auto.service.AutoService;
import com.okta.commons.http.RequestExecutor;
import com.okta.commons.http.RequestExecutorFactory;
import com.okta.commons.http.RetryRequestExecutor;
import com.okta.commons.http.config.HttpClientConfiguration;
import okhttp3.OkHttpClient;

/**
 * @since 1.2.0
 */
@AutoService(RequestExecutorFactory.class)
public class OkHttpRequestExecutorFactory implements RequestExecutorFactory {

    private final OkHttpClient client;

    /**
     * @since 1.2.0
     */
    public OkHttpRequestExecutorFactory() {
        this(null);
    }

    /**
     * Creates an `OkHttpRequestExecutorFactory` that creates a `OkHttpRequestExecutor` instances
     * that uses a shared `OkHttpClient`.
     *
     * @param client a custom configured `OkHttpClient`.
     * @since 1.2.8
     */
    public OkHttpRequestExecutorFactory(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public RequestExecutor create(HttpClientConfiguration clientConfiguration) {
        OkHttpRequestExecutor executor;
        if (client != null) {
            OkHttpClient configuredClient = OkHttpRequestExecutor.configureOkHttpClient(clientConfiguration, client.newBuilder());
            executor = new OkHttpRequestExecutor(clientConfiguration, configuredClient);
        } else {
            executor = new OkHttpRequestExecutor(clientConfiguration);
        }
        return new RetryRequestExecutor(clientConfiguration, executor);
    }
}
