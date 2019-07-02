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
package com.okta.commons.http.config;

import com.okta.commons.http.authc.RequestAuthenticator;


/**
 * This class holds the configuration properties use to construct a {@link com.okta.commons.http.RequestExecutor RequestExecutor}.
 *
 * @since 0.5.0
 */
public class HttpClientConfiguration {

    private String baseUrl;
    private int connectionTimeout;
    private RequestAuthenticator requestAuthenticator;
    private int proxyPort;
    private String proxyHost;
    private String proxyUsername;
    private String proxyPassword;
    private Proxy proxy;
    private int retryMaxElapsed = 0;
    private int retryMaxAttempts = 0;

    public RequestAuthenticator getRequestAuthenticator() {
        return requestAuthenticator;
    }

    public void setRequestAuthenticator(RequestAuthenticator requestAuthenticator) {
        this.requestAuthenticator = requestAuthenticator;
    }
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Connection timeout in seconds
     * @return seconds until connection timeout
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Connection timeout in seconds.
     *
     * @param connectionTimeout the timeout value in seconds
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    public Proxy getProxy() {
        if (this.proxy != null) {
            return proxy;
        }

        Proxy proxy = null;
        // use proxy overrides if they're set
        if ((getProxyPort() > 0 || getProxyHost() != null) && (getProxyUsername() == null || getProxyPassword() == null)) {
            proxy = new Proxy(getProxyHost(), getProxyPort());
        } else if (getProxyUsername() != null && getProxyPassword() != null) {
            proxy = new Proxy(getProxyHost(), getProxyPort(), getProxyUsername(), getProxyPassword());
        }

        this.proxy = proxy;
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
                ", baseUrl='" + baseUrl + '\'' +
                ", connectionTimeout=" + connectionTimeout +
                ", requestAuthenticator=" + requestAuthenticator +
                ", retryMaxElapsed=" + retryMaxElapsed +
                ", retryMaxAttempts=" + retryMaxAttempts +
                ", proxy=" + proxy +
                '}';
    }
}
