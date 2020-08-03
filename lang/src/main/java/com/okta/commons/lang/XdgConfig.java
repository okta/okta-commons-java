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
package com.okta.commons.lang;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class XdgConfig {

    private static final String OKTA = "okta";
    private static final String DOT_OKTA = ".okta";

    private XdgConfig() {}

    public static File getConfigFile(String relativeConfigFile) {
        return new File(getConfigDirectory(), relativeConfigFile);
    }

    public static File getConfigDirectory() {

        List<File> configOptions = new ArrayList<>();
        Map<String, String> envVars = System.getenv();

        // 1. XDG_CONFIG_HOME or default
        configOptions.add(new File(envVars.getOrDefault("XDG_CONFIG_HOME", userHome() + "/.config"), OKTA));

        // 2. check XDG_CONFIG_DIRS
        Optional.ofNullable(envVars.getOrDefault("XDG_CONFIG_DIRS", "/etc/xdg"))
            .map(value -> value.split(File.pathSeparator))
            .ifPresent(values -> Arrays.stream(values).forEachOrdered(value ->
                configOptions.add(new File(value, OKTA))
            ));

        // 3. check legacy ~/.okta
        File defaultDirectory = new File(userHome(), DOT_OKTA);
        configOptions.add(defaultDirectory);

        return configOptions.stream()
            .filter(File::exists)
            .findFirst()
            .orElse(defaultDirectory);
    }

    private static String userHome() {
        return System.getProperty("user.home");
    }
}
