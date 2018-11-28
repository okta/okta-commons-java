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
package com.okta.commons.lang

import org.mockito.Mockito
import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.hasEntry
import static org.hamcrest.Matchers.is

class EnvironmentInfoTest {

    @Test
    void testVersionUtil() {
        assertThat EnvironmentInfo.getEnvironmentInfo(), allOf(
                hasEntry(is("okta-test-lib1"), is("1.2.3")),
                hasEntry(is("okta-test-lib2"), is("v3.2.1")),
                hasEntry(is("java"), is(System.getProperty("java.version"))),
                hasEntry(is(System.getProperty("os.name")), is(System.getProperty("os.version")))
        )
    }

    @Test
    void quickTest() {
        // groovy will access private methods and fields
        def envInfo = new EnvironmentInfo()

        def info = envInfo.getFullEntryStringUsingManifest(Test.class.getName(), "testng")
        assertThat info.name, is("testng")
        assertThat info.version, is("6.9.10") // tied to version in pom.xml

        info = envInfo.getFullEntryStringUsingManifest(Mockito.class.getName(), "mockito")
        assertThat info.name, is("mockito")
        assertThat info.version, is("unknown") // mockito sets "Implementation-Version" in the MANIFEST
    }
}