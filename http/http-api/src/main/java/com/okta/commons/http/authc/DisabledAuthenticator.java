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
package com.okta.commons.http.authc;

import com.okta.commons.http.Request;

/**
 * This {@link RequestAuthenticator} provides a no-op implementation. No authentication is added to requests that use
 * this implementation.
 *
 * @since 1.1.0
 */
@SuppressWarnings("PMD.UnusedFormalParameter")
public class DisabledAuthenticator implements RequestAuthenticator {

    public static final String AUTHENTICATION_SCHEME = "NONE";

    @Override
    public void authenticate(Request request) throws RequestAuthenticationException { }
}
