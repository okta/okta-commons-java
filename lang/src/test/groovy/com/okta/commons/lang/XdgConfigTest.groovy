/*
 * Copyright 2020-Present Okta, Inc.
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

import org.testng.annotations.BeforeTest
import org.testng.annotations.Listeners
import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@Listeners([RestoreEnvironmentVariables, RestoreSystemProperties])
class XdgConfigTest {

    @BeforeTest
    void clearEnvVars() {
        RestoreEnvironmentVariables.clearEnvironmentVariables()
    }

    @Test
    void noEnvVarsNoExistingConfig() {
        assertThat XdgConfig.getConfigFile("okta.yaml"), equalTo(new File(System.getProperty("user.home"), ".okta/okta.yaml"))
    }

    @Test
    void noEnvVarsExistingConfig() {
        // create a config file
        File homeDir = File.createTempDir()
        File expectedConfigFile = new File(homeDir, ".config/okta/okta.yaml")
        expectedConfigFile.getParentFile().mkdirs()
        expectedConfigFile.createNewFile()

        // fake the home directory
        System.setProperty("user.home", homeDir.absolutePath)

        assertThat XdgConfig.getConfigFile("okta.yaml"), equalTo(expectedConfigFile)
    }

    @Test
    void configDirListExistingFile() {
        // create a config file
        File configDir = File.createTempDir()
        File expectedConfigFile = new File(configDir, "okta/okta.yaml")
        expectedConfigFile.getParentFile().mkdirs()
        expectedConfigFile.createNewFile()

        // fake the config directory
        RestoreEnvironmentVariables.setEnvironmentVariable("XDG_CONFIG_DIRS", configDir.absolutePath)

        assertThat XdgConfig.getConfigFile("okta.yaml"), equalTo(expectedConfigFile)
    }
}
