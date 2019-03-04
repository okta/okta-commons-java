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
package com.okta.commons.http;


import java.time.Duration;

/**
 * This class holds the default configuration properties.
 *
 * During application initialization all the properties found in the pre-defined locations that are
 * defined by the user will be added here in the order defined in {@link com.okta.sdk.impl.client.DefaultClientBuilder}.
 * Unset values will use default values from {@code com/okta/sdk/config/okta.yaml}.
 *
 * @since 0.5.0
 */
public class HttpClientConfiguration {

    private RequestAuthenticatorFactory requestAuthenticatorFactory;
    private ClientCredentialsResolver clientCredentialsResolver = ClientCredentialsResolver.DISABLED;
    private Duration connectionTimeout = Duration.ofNanos(0);
    private Duration readTimeout = Duration.ofNanos(0);
    private RequestAuthenticator requestAuthenticator;
    private Proxy proxy;
    private int retryMaxElapsed = 0;
    private int retryMaxAttempts = 0;

    public HttpClientConfiguration() {
        this.requestAuthenticatorFactory = new DefaultRequestAuthenticatorFactory();
        this.setRequestAuthenticator(AuthenticationScheme.NONE, ClientCredentialsResolver.DISABLED.getClientCredentials());
    }

    public ClientCredentialsResolver getClientCredentialsResolver() {
        return clientCredentialsResolver;
    }

    public HttpClientConfiguration setClientCredentialsResolver(ClientCredentialsResolver clientCredentialsResolver) {
        this.clientCredentialsResolver = clientCredentialsResolver;
        return this;
    }

    public RequestAuthenticator getRequestAuthenticator() {
        return requestAuthenticator;
    }

    public HttpClientConfiguration setRequestAuthenticator(RequestAuthenticator requestAuthenticator) {
        this.requestAuthenticator = requestAuthenticator;
        return this;
    }

    public HttpClientConfiguration setRequestAuthenticator(AuthenticationScheme authenticationScheme, ClientCredentials clientCredentials) {
        setRequestAuthenticator(this.requestAuthenticatorFactory.create(authenticationScheme, clientCredentials));
        return this;
    }

    /**
     * Connection timeout in seconds
     * @return seconds until connection timeout
     */
    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Connection timeout in seconds.
     *
     * @param connectionTimeout the timeout value in seconds
     */
    public HttpClientConfiguration setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public HttpClientConfiguration setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public HttpClientConfiguration setProxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    public int getRetryMaxElapsed() {
        return retryMaxElapsed;
    }

    public HttpClientConfiguration setRetryMaxElapsed(int retryMaxElapsed) {
        this.retryMaxElapsed = retryMaxElapsed;
        return this;
    }

    public int getRetryMaxAttempts() {
        return retryMaxAttempts;
    }

    public HttpClientConfiguration setRetryMaxAttempts(int retryMaxAttempts) {
        this.retryMaxAttempts = retryMaxAttempts;
        return this;
    }

    @Override
    public String toString() {
        return "ClientConfiguration{" +
                ", connectionTimeout=" + connectionTimeout +
                ", readTimeout=" + readTimeout +
                ", retryMaxElapsed=" + retryMaxElapsed +
                ", retryMaxAttempts=" + retryMaxAttempts +
                ", proxy=" + proxy +
                '}';
    }
}
