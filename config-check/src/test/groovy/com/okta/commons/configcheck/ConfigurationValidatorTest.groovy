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

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsString

/**
 * Tests for {link ConfigurationValidator}.
 */
class ConfigurationValidatorTest {
    
    @Test
    void nullBaseUrl() {
        def e = expect {ConfigurationValidator.validateHttpsUrl(null)}
        assertThat(e.message,containsString("Your Okta URL is missing"))
    }

    @Test
    void httpBaseUrl() {
        def e = expect {ConfigurationValidator.validateHttpsUrl("http://okta.example.com")}
        assertThat(e.message, containsString("Your Okta URL must start with https"))
    }

    @Test
    void bracketBaseUrl() {
        def e = expect {ConfigurationValidator.validateHttpsUrl("https://{yourOktaDomain}")}
        assertThat(e.message, containsString("Replace {yourOktaDomain} with your Okta domain"))
    }

    @Test
    void adminBaseUrl() {
        def e = expect {ConfigurationValidator.validateHttpsUrl("https://example-admin.okta.com")}
        assertThat(e.message, containsString("Your Okta domain should not contain -admin"))

        e = expect {ConfigurationValidator.validateHttpsUrl("https://example-admin.oktapreview.com")}
        assertThat(e.message, containsString("Your Okta domain should not contain -admin"))

         e = expect {ConfigurationValidator.validateHttpsUrl("https://example-admin.okta-emea.com")}
        assertThat(e.message, containsString("Your Okta domain should not contain -admin"))
    }

    @Test
    void doubleComBaseUrl() {
        def e = expect {ConfigurationValidator.validateHttpsUrl("https://okta.example.com.com")}
        assertThat(e.message, containsString("It looks like there's a typo in your Okta domain"))

        e = expect {ConfigurationValidator.validateHttpsUrl("https://okta.example.com.com/some/path")}
        assertThat(e.message, containsString("It looks like there's a typo in your Okta domain"))

        // this line should NOT throw
        ConfigurationValidator.validateHttpsUrl("https://example.com.commercial.com")
        // xcomxcom would match a regex, so make sure we are matching exactly
        ConfigurationValidator.validateHttpsUrl("https://okta.examplexcomxcom/some/path")
    }

    @Test
    void nullApiToken() {
        def e = expect {ConfigurationValidator.validateApiToken(null)}
        assertThat(e.message, containsString("Your Okta API token is missing"))
    }

    @Test
    void bracketApiToken() {
        def e = expect {ConfigurationValidator.validateApiToken("{apiToken}")}
        assertThat(e.message, containsString("Replace {apiToken} with your Okta API token"))
    }

    @Test
    void validApiToken() {
        // just make sure it doesn't throw
        ConfigurationValidator.validateApiToken("some-other-text")
    }

    @Test
    void nullClientId() {
        def e = expect {ConfigurationValidator.validateClientId(null)}
        assertThat(e.message, containsString("Your client ID is missing"))
    }

    @Test
    void bracketClientId() {
        def e = expect {ConfigurationValidator.validateClientId("{clientId}")}
        assertThat(e.message, containsString("Replace {clientId} with the client ID of your Application"))
    }

    @Test
    void validClientId() {
        // just make sure it doesn't throw
        ConfigurationValidator.validateClientId("some-other-text")
    }


    @Test
    void nullClientSecret() {
        def e = expect {ConfigurationValidator.validateClientSecret(null)}
        assertThat(e.message, containsString("Your client secret is missing"))
    }

    @Test
    void bracketClientSecret() {
        def e = expect {ConfigurationValidator.validateClientSecret("{clientSecret}")}
        assertThat(e.message, containsString("Replace {clientSecret} with the client secret of your Application"))
    }

    @Test
    void validClientSecret() {
        // just make sure it doesn't throw
        ConfigurationValidator.validateClientSecret("some-other-text")
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
