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
package com.okta.commons.configcheck

import org.testng.Assert
import org.testng.annotations.Test

import java.util.concurrent.atomic.AtomicBoolean

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.containsString

/**
 * Tests for {link ConfigurationValidator}.
 */
class ConfigurationValidatorTest {
    
    @Test
    void nullBaseUrl() {
        def e = expect {ConfigurationValidator.assertOrgUrl(null)}
        assertThat(e.message,containsString("Your Okta URL is missing"))
    }

    @Test
    void httpBaseUrl() {
        def e = expect {ConfigurationValidator.assertOrgUrl("http://okta.example.com")}
        assertThat(e.message, allOf(containsString("Your Okta URL must start with https"),
                                   containsString("http://okta.example.com")))
    }

    @Test
    void bracketBaseUrl() {
        def e = expect {ConfigurationValidator.assertOrgUrl("https://{yourOktaDomain}")}
        assertThat(e.message, containsString("Replace {yourOktaDomain} with your Okta domain"))
    }

    @Test
    void adminBaseUrl() {
        def e = expect {ConfigurationValidator.assertOrgUrl("https://example-admin.okta.com")}
        assertThat(e.message, allOf(containsString("Your Okta domain should not contain -admin"),
                                   containsString("https://example-admin.okta.com")))

        e = expect {ConfigurationValidator.assertOrgUrl("https://example-admin.oktapreview.com")}
        assertThat(e.message, containsString("Your Okta domain should not contain -admin"))

         e = expect {ConfigurationValidator.assertOrgUrl("https://example-admin.okta-emea.com")}
        assertThat(e.message, containsString("Your Okta domain should not contain -admin"))
    }

    @Test
    void doubleComBaseUrl() {
        def e = expect {ConfigurationValidator.assertOrgUrl("https://okta.example.com.com")}
        assertThat(e.message, allOf(containsString("It looks like there's a typo in your Okta domain"),
                                   containsString("https://okta.example.com.com")))

        e = expect {ConfigurationValidator.assertOrgUrl("https://okta.example.com.com/some/path")}
        assertThat(e.message, containsString("It looks like there's a typo in your Okta domain"))

        // this line should NOT throw
        ConfigurationValidator.assertOrgUrl("https://example.com.commercial.com")
        // xcomxcom would match a regex, so make sure we are matching exactly
        ConfigurationValidator.assertOrgUrl("https://okta.examplexcomxcom/some/path")
    }

    @Test
    void nullApiToken() {
        def e = expect {ConfigurationValidator.assertApiToken(null)}
        assertThat(e.message, containsString("Your Okta API token is missing"))
    }

    @Test
    void bracketApiToken() {
        def e = expect {ConfigurationValidator.assertApiToken("{apiToken}")}
        assertThat(e.message, containsString("Replace {apiToken} with your Okta API token"))
    }

    @Test
    void validApiToken() {
        // just make sure it doesn't throw
        ConfigurationValidator.assertApiToken("some-other-text")
    }

     @Test
    void nullIssuerUrl() {
        def e = expect {ConfigurationValidator.assertIssuer(null)}
        assertThat(e.message,containsString("Your Okta Issuer URL is missing"))
    }

    @Test
    void httpIssuerUrl() {
        def e = expect {ConfigurationValidator.assertIssuer("http://okta.example.com/oauth/default")}
        assertThat(e.message, allOf(containsString("Your Okta Issuer URL must start with https"),
                                   containsString("http://okta.example.com")))
    }

    @Test
    void bracketIssuerUrl() {
        def e = expect {ConfigurationValidator.assertIssuer("https://{yourOktaDomain}/oauth/default")}
        assertThat(e.message, containsString("Replace {yourOktaDomain} with your Okta domain"))
    }

    @Test
    void adminIssuerUrl() {
        def e = expect {ConfigurationValidator.assertIssuer("https://example-admin.okta.com/oauth/default")}
        assertThat(e.message, allOf(containsString("Your Okta Issuer URL should not contain -admin"),
                                   containsString("https://example-admin.okta.com/oauth/default")))

        e = expect {ConfigurationValidator.assertIssuer("https://example-admin.oktapreview.com/oauth/default")}
        assertThat(e.message, containsString("Your Okta Issuer URL should not contain -admin"))

         e = expect {ConfigurationValidator.assertIssuer("https://example-admin.okta-emea.com/oauth/default")}
        assertThat(e.message, containsString("Your Okta Issuer URL should not contain -admin"))
    }

    @Test
    void doubleComIssuerUrl() {
        def e = expect {ConfigurationValidator.assertIssuer("https://okta.example.com.com/oauth/default")}
        assertThat(e.message, allOf(containsString("It looks like there's a typo in your Okta Issuer URL"),
                                   containsString("https://okta.example.com.com/oauth/default")))

        // this line should NOT throw
        ConfigurationValidator.assertIssuer("https://example.com.commercial.com")
        // xcomxcom would match a regex, so make sure we are matching exactly
        ConfigurationValidator.assertIssuer("https://okta.examplexcomxcom/oauth/default")
    }

    @Test
    void nullClientId() {
        def e = expect {ConfigurationValidator.assertClientId(null)}
        assertThat(e.message, containsString("Your client ID is missing"))
    }

    @Test
    void nullClientIdFunctional() {
        AtomicBoolean result = new AtomicBoolean(true)
        ConfigurationValidator.validateClientId(null).ifInvalid({result.set(it.isValid())})
        assertThat("ifInvalid did not call consumer", !result.get())
    }

    @Test
    void bracketClientId() {
        def e = expect {ConfigurationValidator.assertClientId("{clientId}")}
        assertThat(e.message, containsString("Replace {clientId} with the client ID of your Application"))
    }

    @Test
    void validClientId() {
        // just make sure it doesn't throw
        ConfigurationValidator.assertClientId("some-other-text")
    }

    @Test
    void functionalValidClientId() {
        AtomicBoolean result = new AtomicBoolean(true)
        ConfigurationValidator.validateClientId("some-client-id").ifInvalid({result.set(it.isValid())})
        assertThat("ifInvalid called the consumer, and should not have been with a valid clientId", result.get())
    }

    @Test
    void nullClientSecret() {
        def e = expect {ConfigurationValidator.assertClientSecret(null)}
        assertThat(e.message, containsString("Your client secret is missing"))
    }

    @Test
    void bracketClientSecret() {
        def e = expect {ConfigurationValidator.assertClientSecret("{clientSecret}")}
        assertThat(e.message, containsString("Replace {clientSecret} with the client secret of your Application"))
    }

    @Test
    void validClientSecret() {
        // just make sure it doesn't throw
        ConfigurationValidator.assertClientSecret("some-other-text")
    }
    
    static def expect = { Closure callMe ->
        try {
            callMe.call()
            Assert.fail("Expected ${IllegalArgumentException.getName()} to be thrown.")
        } catch(e) {
            if (!e.class.isAssignableFrom(IllegalArgumentException)) {
                throw e
            }
            return e
        }
    }
    
}
