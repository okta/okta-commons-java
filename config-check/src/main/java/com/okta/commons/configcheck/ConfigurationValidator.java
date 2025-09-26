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
package com.okta.commons.configcheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import static java.util.Locale.ENGLISH;

/**
 * Configuration validation helper class to help validation of common configuration strings.
 *
 * @since 1.0.0
 */
public final class ConfigurationValidator {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationValidator.class);
    private static final ResourceBundle ERRORS = ResourceBundle.getBundle("com/okta/commons/configcheck/configuration-validator");

    private ConfigurationValidator() {}

    /**
     * Asserts the {@code url} is a well formed HTTPS URL and does not contain common typos.  The checks include:
     * <ul>
     * <li>Contains {yourOktaDomain}</li>
     * <li>Hostname ends with .com.com</li>
     * <li>Contains -admin.okta.com</li>
     * <li>Contains -admin.oktapreview.com</li>
     * <li>Contains -admin.okta-emea.com</li>
     * <li>Contains -admin.okta-gov.com</li>
     * <li>Contains -admin.okta.mil</li>
     * <li>Contains -admin.okta-miltest.com</li>
     * <li>Contains -admin.trex-gov.com</li>
     * </ul>
     *
     * @param url The url to be validated
     * @throws IllegalArgumentException Thrown if URL is invalid
     */
    public static void assertOrgUrl(String url) {
        validateOrgUrl(url).ifInvalidThrow();
    }

    /** Asserts the {@code url} is a well formed HTTPS URL and does not contain common typos.  The checks include:
     * <ul>
     * <li>Contains {yourOktaDomain}</li>
     * <li>Hostname ends with .com.com</li>
     * <li>Contains -admin.okta.com</li>
     * <li>Contains -admin.oktapreview.com</li>
     * <li>Contains -admin.okta-emea.com</li>
     * <li>Contains -admin.okta-gov.com</li>
     * <li>Contains -admin.okta.mil</li>
     * <li>Contains -admin.okta-miltest.com</li>
     * <li>Contains -admin.trex-gov.com</li>
     * </ul>
     *
     * @param url The url to be validated
     * @param allowNonHttpsForTesting Allow orgUrl to be non-https, likely used for testing.
     * @throws IllegalArgumentException Thrown if URL is invalid
     * @deprecated use {@link #validateOrgUrl(String)} instead, disabling this check is NOT recommended, and should
     * ONLY be done in testing scenarios
     */
    @Deprecated
    public static void assertOrgUrl(String url, boolean allowNonHttpsForTesting) {
        validateOrgUrl(url, allowNonHttpsForTesting).ifInvalidThrow();
    }

    /**
     * Returns a {@link ValidationResponse} checking to make sure the {@code url} is a well formed HTTPS URL and does
     * not contain common typos.  The checks include:
     * <ul>
     * <li>Contains {yourOktaDomain}</li>
     * <li>Hostname ends with .com.com</li>
     * <li>Contains -admin.okta.com</li>
     * <li>Contains -admin.oktapreview.com</li>
     * <li>Contains -admin.okta-emea.com</li>
     * <li>Contains -admin.okta-gov.com</li>
     * <li>Contains -admin.okta.mil</li>
     * <li>Contains -admin.okta-miltest.com</li>
     * <li>Contains -admin.trex-gov.com</li>
     * </ul>
     *
     * @param url The url to be validated
     * @return a ValidationResponse containing the validation status and message (when invalid)
     */
    public static ValidationResponse validateOrgUrl(String url) {
        return validateHttpsUrl(url, "orgUrl", false);
    }

    /**
     * Returns a {@link ValidationResponse} checking to make sure the {@code url} is a well formed HTTPS URL and does
     * not contain common typos.  The checks include:
     * <ul>
     * <li>Contains {yourOktaDomain}</li>
     * <li>Hostname ends with .com.com</li>
     * <li>Contains -admin.okta.com</li>
     * <li>Contains -admin.oktapreview.com</li>
     * <li>Contains -admin.okta-emea.com</li>
     * <li>Contains -admin.okta-gov.com</li>
     * <li>Contains -admin.okta.mil</li>
     * <li>Contains -admin.okta-miltest.com</li>
     * <li>Contains -admin.trex-gov.com</li>
     * </ul>
     * *
     * @param url The url to be validated
     * @param allowNonHttpsForTesting Allow orgUrl to be non-https, likely used for testing.
     * @return a ValidationResponse containing the validation status and message (when invalid)
     * @deprecated use {@link #validateOrgUrl(String)} instead, disabling this check is NOT recommended, and should
     * ONLY be done in testing scenarios
     */
    @Deprecated
    public static ValidationResponse validateOrgUrl(String url, boolean allowNonHttpsForTesting) {
        return validateHttpsUrl(url, "orgUrl", allowNonHttpsForTesting);
    }

    /**
     * Asserts that an API token is not null and contains does not contain the string {@code {apiToken}}.
     *
     * @param token The API Token to be validated
     * @throws IllegalArgumentException Thrown if {@code token} is invalid
     */
    public static void assertApiToken(String token) {
        validateApiToken(token).ifInvalidThrow();
    }

    /**
     * Returns a {@link ValidationResponse} checking to make sure the API token is not null and
     * contains does not contain the string {@code {apiToken}}.
     *
     * @param token The API Token to be validated
     * @return a ValidationResponse containing the validation status and message (when invalid)
     */

    public static ValidationResponse validateApiToken(String token) {
        ValidationResponse response = new ValidationResponse();
        if (!hasText(token)) {
            response.setMessage(ERRORS.getString("apiToken.missing"));
        } else if (containsCaseInsensitive(token, "{apitoken}")) {
            response.setMessage(ERRORS.getString("apiToken.containsBrackets"));
        }
        return response;
    }

    /**
     * Asserts the {@code url} is a well formed HTTPS URL and does not contain common typos.  The checks include:
     * <ul>
     * <li>Contains {yourOktaDomain}</li>
     * <li>Hostname ends with .com.com</li>
     * <li>Contains -admin.okta.com</li>
     * <li>Contains -admin.oktapreview.com</li>
     * <li>Contains -admin.okta-emea.com</li>
     * <li>Contains -admin.okta-gov.com</li>
     * <li>Contains -admin.okta.mil</li>
     * <li>Contains -admin.okta-miltest.com</li>
     * <li>Contains -admin.trex-gov.com</li>
     * </ul>
     *
     * @param url The url to be validated
     * @throws IllegalArgumentException Thrown if URL is invalid
     */
    public static void assertIssuer(String url) {
        validateIssuer(url).ifInvalidThrow();
    }

    /**
     * Returns a {@link ValidationResponse} checking the {@code url} is a well formed HTTPS URL and
     * does not contain common typos.  The checks include:
     * <ul>
     * <li>Contains {yourOktaDomain}</li>
     * <li>Hostname ends with .com.com</li>
     * <li>Contains -admin.okta.com</li>
     * <li>Contains -admin.oktapreview.com</li>
     * <li>Contains -admin.okta-emea.com</li>
     * <li>Contains -admin.okta-gov.com</li>
     * <li>Contains -admin.okta.mil</li>
     * <li>Contains -admin.okta-miltest.com</li>
     * <li>Contains -admin.trex-gov.com</li>
     * </ul>
     *
     * @param url The url to be validated
     * @return a ValidationResponse containing the validation status and message (when invalid)
     */
    public static ValidationResponse validateIssuer(String url) {
        return validateHttpsUrl(url, "issuerUrl", false);
    }

    /**
     * Asserts that a {@code clientId} is not null and contains does not contain the string {@code {clientId}}.
     *
     * @param clientId The client Id to be validated
     * @throws IllegalArgumentException Thrown if URL is invalid
     */
    public static void assertClientId(String clientId) {
        validateClientId(clientId).ifInvalidThrow();
    }

    /**
     * Returns a {@link ValidationResponse} checking the {@code clientId} is not null and contains does not contain the string {@code {apiToclientIdken}}.
     *
     * @param clientId The client Id to be validated
     * @return a ValidationResponse containing the validation status and message (when invalid)
     */
    public static ValidationResponse validateClientId(String clientId) {
        ValidationResponse response = new ValidationResponse();
        if (!hasText(clientId)) {
            response.setMessage(ERRORS.getString("clientId.missing"));
        } else if (containsCaseInsensitive(clientId, "{clientid}")) {
            response.setMessage(ERRORS.getString("clientId.containsBrackets"));
        }
        return response;
    }

    /**
     * Asserts that a client secret is not null and contains does not contain the string {@code {clientSecret}}.
     *
     * @param clientSecret the Client Secret to be validated
     * @throws IllegalArgumentException Thrown if URL is invalid
     */
    public static void assertClientSecret(String clientSecret) {
        validateClientSecret(clientSecret).ifInvalidThrow();
    }

    /**
     * Returns a {@link ValidationResponse} checking the {@code clientSecret} is not null and contains does not contain the string {@code {clientSecret}}.
     *
     * @param clientSecret the Client Secret to be validated
     * @return a ValidationResponse containing the validation status and message (when invalid)
     */
    public static ValidationResponse validateClientSecret(String clientSecret) {
        ValidationResponse response = new ValidationResponse();
        if (!hasText(clientSecret)) {
            response.setMessage(ERRORS.getString("clientSecret.missing"));
        } else if (containsCaseInsensitive(clientSecret, "{clientsecret}")) {
            response.setMessage(ERRORS.getString("clientSecret.containsBrackets"));
        }
        return response;
    }

    private static String formattedErrorMessage(String messageKey, Object... args) {
        String message = ERRORS.getString(messageKey);
        return MessageFormat.format(message, args);
    }

    private static ValidationResponse validateHttpsUrl(String url, String keyPrefix, boolean allowNonHttps) {
        ValidationResponse response = new ValidationResponse();
        if (!hasText(url)) {
            response.setMessage(ERRORS.getString(keyPrefix + ".missing"));
        } else if (containsCaseInsensitive(url, "{youroktadomain}")) {
            response.setMessage(ERRORS.getString(keyPrefix + ".containsBrackets"));
        } else {
            try {
                URL tempUrl = new URL(url);
                String host = tempUrl.getHost().toLowerCase(ENGLISH);
                if (!"https".equalsIgnoreCase(tempUrl.getProtocol()) ){
                    if (allowNonHttps) {
                        logger.warn(ERRORS.getString("httpsCheck.disabled"));
                    } else {
                        response.setMessage(formattedErrorMessage(keyPrefix + ".nonHttpsInvalid", url));
                    }
                } else if (host.endsWith(".com.com")){
                    response.setMessage(formattedErrorMessage(keyPrefix + ".invalid", url));
                } else if (host.endsWith("-admin.okta.com")
                    || host.endsWith("-admin.oktapreview.com")
                    || host.endsWith("-admin.okta-emea.com")
                    || host.endsWith("-admin.okta-gov.com")
                    || host.endsWith("-admin.okta.mil")
                    || host.endsWith("-admin.okta-miltest.com")
                    || host.endsWith("-admin.trex-gov.com")) {
                    response.setMessage(formattedErrorMessage(keyPrefix + ".containsAdmin", url));
                }
            } catch (MalformedURLException e) {
                response.setMessage(formattedErrorMessage(keyPrefix + ".invalid", url))
                    .setException(e);
            }
        }

        return response;
    }

    /*
     *  private methods copied from com.okta.sdk.lang.Assert, can be updated if we pull that out of the SDK project
     */

    private static boolean containsCaseInsensitive(String textToSearch, String substring) {
        return hasLength(textToSearch)
                && hasLength(substring)
                && textToSearch.toLowerCase(ENGLISH).contains(substring);
    }

    private static boolean hasText(CharSequence str) {
        if (!hasLength(str)) {
            return false;
        }

        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLength(CharSequence str) {
        return str != null && str.length() > 0;
    }
}