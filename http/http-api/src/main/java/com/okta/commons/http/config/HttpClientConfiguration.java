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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


/**
 * This class holds the configuration properties use to construct a {@link com.okta.commons.http.RequestExecutor RequestExecutor}.
 *
 * @since 0.5.0
 */
public class HttpClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(HttpClientConfiguration.class);

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
    private final Map<String, String> requestExecutorParams = new HashMap<>();

    public static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = Integer.MAX_VALUE/2;
    public static final int DEFAULT_MAX_CONNECTIONS_TOTAL = Integer.MAX_VALUE;
    public static final int DEFAULT_CONNECTION_VALIDATION_INACTIVITY = 2000; // 2sec
    public static final int DEFAULT_CONNECTION_TIME_TO_LIVE = 5 * 1000 * 60; // 5 minutes

    public static final String MAX_CONNECTIONS_PER_ROUTE_PROPERTY_KEY = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.maxPerRoute";
    public static final String MAX_CONNECTIONS_TOTAL_PROPERTY_KEY = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.maxTotal";
    public static final String CONNECTION_VALIDATION_PROPERTY_KEY = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.validateAfterInactivity";
    public static final String CONNECTION_TIME_TO_LIVE_PROPERTY_KEY = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.timeToLive";

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

    public void setRequestExecutorParams(Map<String, String> map) {
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                this.requestExecutorParams.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public int getMaxConnectionPerRoute() {
        return getRequestExecutorParam(
            "maxPerRoute",
            "Bad max connection per route value",
            DEFAULT_MAX_CONNECTIONS_PER_ROUTE
        );
    }

    public int getMaxConnectionTotal() {
        return getRequestExecutorParam(
            "maxTotal",
            "Bad max connection total value",
            DEFAULT_MAX_CONNECTIONS_TOTAL
        );
    }

    public int getMaxConnectionInactivity() {
        return getRequestExecutorParam(
            "validateAfterInactivity",
            "Invalid max connection inactivity validation value",
            DEFAULT_CONNECTION_VALIDATION_INACTIVITY
        );
    }

    public int getConnectionTimeToLive() {
        return getRequestExecutorParam(
            "timeToLive",
            "Invalid connection time to live value",
            DEFAULT_CONNECTION_TIME_TO_LIVE
        );
    }

    public int getRequestExecutorParam(String key, String warning, int defaultValue) {
        String configuredValueString = this.requestExecutorParams.get(key);
        try {
            if (configuredValueString != null) {
                return Integer.parseInt(configuredValueString);
            }
        } catch (NumberFormatException nfe) {
            log.warn("{}: {}. Using default: {}.",
                warning,
                configuredValueString,
                defaultValue,
                nfe);
        }
        return defaultValue;
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
                ", maxPerRoute=" + getMaxConnectionPerRoute() +
                ", maxTotal=" + getMaxConnectionTotal() +
                ", validateAfterInactivity=" + getMaxConnectionInactivity() +
                ", timeToLive=" + getConnectionTimeToLive() +
                '}';
    }
}
