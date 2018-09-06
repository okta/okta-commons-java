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
import java.util.ResourceBundle;

/**
 * Configuration validation helper class to help validation of common configuration strings.
 *
 * @since 1.0.0
 */
public final class ConfigurationValidator {

    private static final ResourceBundle ERRORS = ResourceBundle.getBundle("com/okta/commons/configcheck/ConfigurationValidator");

    private ConfigurationValidator() {}

    /**
     * Validates the {code url} is a well formed HTTPS URL and does not contain common typos.  The checks include:
     * <ul>
     *     <li>Contains {yourOktaDomain}</li>
     *     <li>Hostname ends with .com.com</li>
     *     <li>Contains -admin.okta.com</li>
     *     <li>Contains -admin.oktapreview.com</li>
     *     <li>Contains -admin.okta-emea.com</li>
     * </ul>
     * @param url The url to be validated
     */
    public static void validateHttpsUrl(String url) {

        hasText(url, ERRORS.getString("orgUrl.missing"));
        doesNotContain(url, "{yourOktaDomain}", ERRORS.getString("orgUrl.containsBrackets"));
        doesNotContain(url, "-admin.okta.com", ERRORS.getString("orgUrl.containsAdmin"));
        doesNotContain(url, "-admin.oktapreview.com", ERRORS.getString("orgUrl.containsAdmin"));
        doesNotContain(url, "-admin.okta-emea.com", ERRORS.getString("orgUrl.containsAdmin"));

        try {
            URL tempUrl = new URL(url);
            if (!"https".equalsIgnoreCase(tempUrl.getProtocol())) {
                throw new IllegalArgumentException(ERRORS.getString("orgUrl.nonHttpsInvalid"));
            }
            if (tempUrl.getHost().endsWith(".com.com")){
                throw new IllegalArgumentException(ERRORS.getString("orgUrl.invalid"));
            }

        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(ERRORS.getString("orgUrl.invalid"), e);
        }
    }

    /**
     * Validates that an API token is not null and contains does not contain the string {@code {apiToken}}.
     * @param token The API Token to be validated
     */
    public static void validateApiToken(String token) {
        hasText(token, ERRORS.getString("apiToken.missing"));
        doesNotContain(token, "{apiToken}",  ERRORS.getString("apiToken.invalid"));
    }

    /**
     * Validates that a client Id is not null and contains does not contain the string {@code {apiToken}}.
     * @param clientId The Client Id to be validated
     */
    public static void validateClientId(String clientId) {
        hasText(clientId, ERRORS.getString("clientId.missing"));
        doesNotContain(clientId, "{clientId}",  ERRORS.getString("clientId.containsBrackets"));
    }

    /**
     * Validates that a client secret is not null and contains does not contain the string {@code {clientSecret}}.
     * @param clientSecret the Client Secret to be validated
     */
    public static void validateClientSecret(String clientSecret) {
        hasText(clientSecret, ERRORS.getString("clientSecret.missing"));
        doesNotContain(clientSecret, "{clientSecret}",  ERRORS.getString("clientSecret.containsBrackets"));
    }

    /*
     *  private methods copied from com.okta.sdk.lang.Assert, can be updated if we pull that out of the SDK project
     */

    private static void doesNotContain(String textToSearch, String substring, String message) {
        if (hasLength(textToSearch) && hasLength(substring) &&
                textToSearch.contains(substring)) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void hasText(String text, String message) {
        if (!hasText(text)) {
            throw new IllegalArgumentException(message);
        }
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