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

    private Integer maxConnectionPerRoute;
    private Integer maxConnectionTotal;
    private Integer connectionValidationInactivity;
    private Integer connectionTimeToLive;

    private static final Logger log = LoggerFactory.getLogger(HttpClientConfiguration.class);

    private static final String MAX_CONNECTIONS_PER_ROUTE_PROPERTY_KEY = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.maxPerRoute";
    private static final int MAX_CONNECTIONS_PER_ROUTE_DEFAULT_VALUE = Integer.MAX_VALUE/2;
    private static final String MAX_CONNECTIONS_PER_ROUTE_WARNING_MESSAGE = "Bad max connection per route value";

    private static final String MAX_CONNECTIONS_TOTAL_PROPERTY_KEY = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.maxTotal";
    private static final int MAX_CONNECTIONS_TOTAL_DEFAULT_VALUE = Integer.MAX_VALUE;
    private static final String MAX_CONNECTIONS_TOTAL_WARNING_MESSAGE = "Bad max connection total value";

    private static final String CONNECTION_VALIDATION_INACTIVITY_PROPERTY_KEY = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.validateAfterInactivity";
    private static final int CONNECTION_VALIDATION_INACTIVITY_DEFAULT_VALUE = 2000; // 2sec
    private static final String CONNECTION_VALIDATION_INACTIVITY_WARNING_MESSAGE = "Invalid max connection inactivity validation value";

    private static final String CONNECTION_TIME_TO_LIVE_PROPERTY_KEY = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.timeToLive";
    private static final int CONNECTION_TIME_TO_LIVE_DEFAULT_VALUE = 5 * 1000 * 60; // 5 minutes
    private static final String CONNECTION_TIME_TO_LIVE_WARNING_MESSAGE = "Invalid connection time to live value";

    public String getMaxConnectionsTotalPropertyKey() {
        return MAX_CONNECTIONS_TOTAL_PROPERTY_KEY;
    }

    public String getMaxConnectionsPerRoutePropertyKey() {
        return MAX_CONNECTIONS_PER_ROUTE_PROPERTY_KEY;
    }

    public Integer getMaxConnectionPerRouteDefault() {
        return MAX_CONNECTIONS_PER_ROUTE_DEFAULT_VALUE;
    }

    public Integer getMaxConnectionTotalDefault() {
        return MAX_CONNECTIONS_TOTAL_DEFAULT_VALUE;
    }

    public Integer getMaxConnectionPerRoute() {
        if(this.maxConnectionPerRoute == null) {
            this.maxConnectionPerRoute = parseConfigValue(
                MAX_CONNECTIONS_PER_ROUTE_PROPERTY_KEY,
                MAX_CONNECTIONS_PER_ROUTE_DEFAULT_VALUE,
                MAX_CONNECTIONS_PER_ROUTE_WARNING_MESSAGE);
        }
        return this.maxConnectionPerRoute;
    }

    public Integer getMaxConnectionTotal() {
        if(this.maxConnectionTotal == null) {
            this.maxConnectionTotal = parseConfigValue(
                MAX_CONNECTIONS_TOTAL_PROPERTY_KEY,
                MAX_CONNECTIONS_TOTAL_DEFAULT_VALUE,
                MAX_CONNECTIONS_TOTAL_WARNING_MESSAGE);
        }
        return this.maxConnectionTotal;
    }

    public Integer getConnectionValidationInactivity() {
        if(this.connectionValidationInactivity == null) {
            this.connectionValidationInactivity = parseConfigValue(
                CONNECTION_VALIDATION_INACTIVITY_PROPERTY_KEY,
                CONNECTION_VALIDATION_INACTIVITY_DEFAULT_VALUE,
                CONNECTION_VALIDATION_INACTIVITY_WARNING_MESSAGE);
        }
        return this.connectionValidationInactivity;
    }

    public int getConnectionTimeToLive() {
        if(this.connectionTimeToLive == null) {
            this.connectionTimeToLive = parseConfigValue(
                CONNECTION_TIME_TO_LIVE_PROPERTY_KEY,
                CONNECTION_TIME_TO_LIVE_DEFAULT_VALUE,
                CONNECTION_TIME_TO_LIVE_WARNING_MESSAGE);
        }
        return this.connectionTimeToLive;
    }

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

    private int parseConfigValue(String key, int defaultValue, String warning) {

        int configuredValue = defaultValue;
        String configuredValueString = System.getProperty(key);
        if (configuredValueString != null) {
            try {
                configuredValue = Integer.parseInt(configuredValueString);
            } catch (NumberFormatException nfe) {
                log.warn("{}: {}. Using default: {}.", warning, configuredValueString, defaultValue, nfe);
            }
        }
        return configuredValue;
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
