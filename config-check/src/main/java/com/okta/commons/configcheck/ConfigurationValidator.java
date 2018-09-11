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

    private static final ResourceBundle ERRORS = ResourceBundle.getBundle("com/okta/commons/configcheck/ConfigurationValidator");

    private ConfigurationValidator() {}

    /**
     * Validates the {@code url} is a well formed HTTPS URL and does not contain common typos.  The checks include:
     * <ul>
     *     <li>Contains {yourOktaDomain}</li>
     *     <li>Hostname ends with .com.com</li>
     *     <li>Contains -admin.okta.com</li>
     *     <li>Contains -admin.oktapreview.com</li>
     *     <li>Contains -admin.okta-emea.com</li>
     * </ul>
     * @param url The url to be validated
     */
    public static void assertOrgUrl(String url) {
        validateOrgUrl(url).ifInvalidThrow();
    }

    public static ValidationResponse validateOrgUrl(String url) {
        return validateHttpsUrl(url, "orgUrl");
    }

    /**
     * Validates that an API token is not null and contains does not contain the string {@code {apiToken}}.
     * @param token The API Token to be validated
     */
    public static void assertApiToken(String token) {
        validateApiToken(token).ifInvalidThrow();
    }

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
     * Validates the {@code url} is a well formed HTTPS URL and does not contain common typos.  The checks include:
     * <ul>
     *     <li>Contains {yourOktaDomain}</li>
     *     <li>Hostname ends with .com.com</li>
     *     <li>Contains -admin.okta.com</li>
     *     <li>Contains -admin.oktapreview.com</li>
     *     <li>Contains -admin.okta-emea.com</li>
     * </ul>
     * @param url The url to be validated
     */
    public static void assertIssuer(String url) {
        validateIssuer(url).ifInvalidThrow();
    }

    public static ValidationResponse validateIssuer(String url) {
        return validateHttpsUrl(url, "issuerUrl");
    }

    /**
     * Validates that a client Id is not null and contains does not contain the string {@code {apiToken}}.
     * @param clientId The Client Id to be validated
     */
    public static void assertClientId(String clientId) {
        validateClientId(clientId).ifInvalidThrow();
    }

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
     * Validates that a client secret is not null and contains does not contain the string {@code {clientSecret}}.
     * @param clientSecret the Client Secret to be validated
     */
    public static void assertClientSecret(String clientSecret) {
        validateClientSecret(clientSecret).ifInvalidThrow();
    }

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

    private static ValidationResponse validateHttpsUrl(String url, String keyPrefix) {
        ValidationResponse response = new ValidationResponse();
        if (!hasText(url)) {
            response.setMessage(ERRORS.getString(keyPrefix + ".missing"));
        } else if (containsCaseInsensitive(url, "{youroktadomain}")) {
            response.setMessage(ERRORS.getString(keyPrefix + ".containsBrackets"));
        } else {
            try {
                URL tempUrl = new URL(url);
                String host = tempUrl.getHost().toLowerCase(ENGLISH);
                if (!"https".equalsIgnoreCase(tempUrl.getProtocol())) {
                    response.setMessage(formattedErrorMessage(keyPrefix + ".nonHttpsInvalid", url));
                } else if (host.endsWith(".com.com")){
                    response.setMessage(formattedErrorMessage(keyPrefix + ".invalid", url));
                } else if (host.endsWith("-admin.okta.com")
                        || host.endsWith("-admin.oktapreview.com")
                        || host.endsWith("-admin.okta-emea.com")){
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