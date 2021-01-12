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
import com.okta.commons.lang.Strings;
import com.okta.sdk.impl.config.*;
import com.okta.sdk.impl.io.ClasspathResource;
import com.okta.sdk.impl.io.DefaultResourceFactory;
import com.okta.sdk.impl.io.Resource;
import com.okta.sdk.impl.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * This class holds the configuration properties use to construct a {@link com.okta.commons.http.RequestExecutor RequestExecutor}.
 *
 * @since 0.5.0
 */
public class HttpClientConfiguration implements ClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(HttpClientConfiguration.class);

    private static final String ENVVARS_TOKEN   = "envvars";
    private static final String SYSPROPS_TOKEN  = "sysprops";
    private static final String OKTA_CONFIG_CP  = "com/okta/sdk/config/";
    private static final String OKTA_YAML       = "okta.yaml";
    private static final String OKTA_PROPERTIES = "okta.properties";

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
    private int maxConnectionPerRoute;
    private int maxConnectionTotal;
    private int connectionValidationInactivity;
    private int connectionTimeToLive;

    public HttpClientConfiguration() {
        this(new DefaultResourceFactory());
    }

    HttpClientConfiguration(ResourceFactory resourceFactory) {
        Collection<PropertiesSource> sources = new ArrayList<>();

        for (String location : configSources()) {

            if (ENVVARS_TOKEN.equalsIgnoreCase(location)) {
                sources.add(EnvironmentVariablesPropertiesSource.oktaFilteredPropertiesSource());
            } else if (SYSPROPS_TOKEN.equalsIgnoreCase(location)) {
                sources.add(SystemPropertiesSource.oktaFilteredPropertiesSource());
            } else {
                Resource resource = resourceFactory.createResource(location);

                PropertiesSource wrappedSource;
                if (Strings.endsWithIgnoreCase(location, ".yaml")) {
                    wrappedSource = new YAMLPropertiesSource(resource);
                } else {
                    wrappedSource = new ResourcePropertiesSource(resource);
                }

                PropertiesSource propertiesSource = new OptionalPropertiesSource(wrappedSource);
                sources.add(propertiesSource);
            }
        }

        Map<String, String> props = new LinkedHashMap<>();

        for (PropertiesSource source : sources) {
            Map<String, String> srcProps = source.getProperties();
            props.putAll(srcProps);
        }

        this.setMaxConnectionPerRoute(
            tryParse(
                props.get(MAX_CONNECTIONS_PER_ROUTE_PROPERTY_NAME),
                MAX_CONNECTIONS_PER_ROUTE_PROPERTY_VALUE_DEFAULT)
        );

        this.setMaxConnectionTotal(
            tryParse(
                props.get(MAX_CONNECTIONS_TOTAL_PROPERTY_NAME),
                MAX_CONNECTIONS_TOTAL_PROPERTY_VALUE_DEFAULT)
        );

        this.setConnectionValidationInactivity(
            tryParse(
                props.get(CONNECTION_VALIDATION_INACTIVITY_PROPERTY_NAME),
                CONNECTION_VALIDATION_INACTIVITY_PROPERTY_VALUE_DEFAULT)
        );

        this.setConnectionTimeToLive(
            tryParse(
                props.get(CONNECTION_TIME_TO_LIVE_PROPERTY_NAME),
                CONNECTION_TIME_TO_LIVE_PROPERTY_VALUE_DEFAULT)
        );
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

    public Integer getMaxConnectionPerRoute() {
        return this.maxConnectionPerRoute;
    }

    public HttpClientConfiguration setMaxConnectionPerRoute(int maxConnectionPerRoute) {
        this.maxConnectionPerRoute = maxConnectionPerRoute;
        return this;
    }

    public Integer getMaxConnectionTotal() {
        return this.maxConnectionTotal;
    }

    public HttpClientConfiguration setMaxConnectionTotal(int maxConnectionTotal) {
        this.maxConnectionTotal = maxConnectionTotal;
        return this;
    }

    public Integer getConnectionValidationInactivity() {
        return this.connectionValidationInactivity;
    }

    public HttpClientConfiguration setConnectionValidationInactivity(int connectionValidationInactivity) {
        this.connectionValidationInactivity = connectionValidationInactivity;
        return this;
    }

    public int getConnectionTimeToLive() {
        return this.connectionTimeToLive;
    }

    public HttpClientConfiguration setConnectionTimeToLive(int connectionTimeToLive) {
        this.connectionTimeToLive = connectionTimeToLive;
        return this;
    }

    private static String[] configSources() {

        // lazy load the config sources as the user.home system prop could change for testing
        return new String[] {
            ClasspathResource.SCHEME_PREFIX + OKTA_CONFIG_CP + OKTA_PROPERTIES,
            ClasspathResource.SCHEME_PREFIX + OKTA_CONFIG_CP + OKTA_YAML,
            ClasspathResource.SCHEME_PREFIX + OKTA_PROPERTIES,
            ClasspathResource.SCHEME_PREFIX + OKTA_YAML,
            System.getProperty("user.home") + File.separatorChar + ".okta" + File.separatorChar + OKTA_YAML,
            ENVVARS_TOKEN,
            SYSPROPS_TOKEN
        };
    }

    private int tryParse(String value, int defaultVal) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
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
                ", maxConnectionPerRoute=" + maxConnectionPerRoute +
                ", maxConnectionTotal=" + maxConnectionTotal +
                ", connectionValidationInactivity=" + connectionValidationInactivity +
                ", connectionTimeToLive=" + connectionTimeToLive +
                '}';
    }
}
