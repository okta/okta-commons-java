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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * @since 0.5.0
 */
public final class RequestUtils {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC);

    private RequestUtils() {}

    /**
     * Returns {@code true} if the specified URI uses a standard port (i.e. http == 80 or https == 443),
     * {@code false} otherwise.
     *
     * @param uri true if the specified URI uses a standard port
     * @return true if the specified URI is using a non-standard port, false otherwise
     */
    @SuppressWarnings("PMD.UselessParentheses")
    public static boolean isDefaultPort(URI uri) {
        String scheme = uri.getScheme().toLowerCase(Locale.ENGLISH);
        int port = uri.getPort();
        return port <= 0
                || (port == 80 && scheme.equals("http"))
                || (port == 443 && scheme.equals("https"));
    }

    public static String encodeUrl(String value, boolean path, boolean canonical) {
        if (value == null || value.equals("")) {
            return "";
        }

        String encoded;

        try {
            encoded = URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("Unable to UTF-8 encode url string component [" + value + "]", ex);
        }

        if (canonical) {
            encoded = encoded.replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~"); //yes, this is reversed (compared to the 2 above it) intentionally

            if (path) {
                encoded = encoded.replace("%2F", "/");
            }
        }

        return encoded;
    }

    /**
     * Convert supplied Date object into ISO 8601 formatted
     * date string (e.g. 2017-11-30T21:15:16Z) that Okta core supports.
     *
     * @param inDate
     * @return ISO formatted string
     */
    public static String getFormattedDate(Date inDate) {
        return DATE_TIME_FORMATTER.format(inDate.toInstant());
    }


    public static String fetchHeaderValueAndRemoveIfPresent(Request request, String headerName) {
        String result = null;
        if(request.getHeaders().toSingleValueMap().containsKey(headerName)) {
            result = request.getHeaders().toSingleValueMap().get(headerName);
            request.getHeaders().remove(headerName);
        }
        return result;
    }
}
