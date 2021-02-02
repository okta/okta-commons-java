/*
 * Copyright 2019-Present Okta, Inc.
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
package com.okta.commons.http.httpclient

import com.okta.commons.http.config.HttpClientConfiguration
import org.testng.Assert
import org.testng.IHookCallBack
import org.testng.IHookable
import org.testng.ITestResult
import org.testng.SkipException
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is

class HttpClientRequestExecutorStaticConfigTest implements IHookable {

    @Test(dataProvider = "validateAfterInactivity")
    void validateAfterInactivitySysPropOnly(String configValue, int expectedValue) {
        def prop = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.validateAfterInactivity"
        if (configValue != null) {
            System.properties.setProperty(prop, configValue)
        }
        def requestExecutor = loadHttpClientRequestExecutor()
        assertThat requestExecutor.getMaxConnectionInactivity(), is(expectedValue)
    }

    @Test(dataProvider = "timeToLive")
    void timeToLiveSysPropOnly(String configValue, int expectedValue) {
        def prop = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.timeToLive"
        if (configValue != null) {
            System.properties.setProperty(prop, configValue)
        }
        def requestExecutor = loadHttpClientRequestExecutor()
        assertThat requestExecutor.getConnectionTimeToLive(), is(expectedValue)
    }

    @Test(dataProvider = "maxConnections")
    void maxConnectionsSysPropOnly(String configValue, int expectedValue) {
        def prop = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.maxTotal"
        if (configValue != null) {
            System.properties.setProperty(prop, configValue)
        }
        def requestExecutor = loadHttpClientRequestExecutor()
        assertThat requestExecutor.getMaxConnectionTotal(), is(expectedValue)
    }

    @Test(dataProvider = "maxConnectionsPerRoute")
    void maxConnectionsPerRouteSysPropOnly(String configValue, int expectedValue) {
        def prop = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.maxPerRoute"
        if (configValue != null) {
            System.properties.setProperty(prop, configValue)
        }
        if(configValue == "0" || configValue == "-1") {
            def exception = expect(IllegalArgumentException, {loadHttpClientRequestExecutor()})
            assertThat exception.getMessage(), is("Max per route value may not be negative or zero")
        } else {
            def requestExecutor = loadHttpClientRequestExecutor()
            assertThat requestExecutor.getMaxConnectionPerRoute(), is(expectedValue)
        }
    }

    @Test(dataProvider = "validateAfterInactivityParams", enabled = false)
    void validateAfterInactivitySysPropAndParams(String paramValue, String sysPropValue, int expectedValue) {
        def prop = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.validateAfterInactivity"
        if(sysPropValue != null) {
            System.properties.setProperty(prop, sysPropValue)
        }
        def reqExec = buildHttpClientRequestExecutorWithParams("validateAfterInactivity", paramValue)
        assertThat reqExec.getMaxConnectionInactivity(), is(expectedValue)
    }

    @Test(dataProvider = "timeToLiveParams", enabled = false)
    void timeToLiveSysPropAndParams(String paramValue, String sysPropValue, int expectedValue) {
        def prop = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.timeToLive"
        if(sysPropValue != null) {
            System.properties.setProperty(prop, sysPropValue)
        }
        def reqExec = buildHttpClientRequestExecutorWithParams("timeToLive", paramValue)
        assertThat reqExec.getConnectionTimeToLive(), is(expectedValue)
    }

    @Test(dataProvider = "maxConnectionsTotalParams", enabled = false)
    void maxConnectionsSysPropAndParams(String paramValue, String sysPropValue, int expectedValue) {
        def prop = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.maxTotal"
        if(sysPropValue != null) {
            System.properties.setProperty(prop, sysPropValue)
        }
        def reqExec = buildHttpClientRequestExecutorWithParams("maxConnectionsTotal", paramValue)
        assertThat reqExec.getMaxConnectionTotal(), is(expectedValue)
    }

    @Test(dataProvider = "maxConnectionsPerRouteParams", enabled = false)
    void maxConnectionsPerRouteSysPropAndParams(String paramValue, String sysPropValue, int expectedValue) {
        def prop = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.maxPerRoute"
        if (sysPropValue != null) {
            System.properties.setProperty(prop, sysPropValue)
        }
        if((paramValue == "0" || paramValue == "-1") || (sysPropValue == "0" || sysPropValue == "-1")) {
            def exception = expect(IllegalArgumentException, {
                buildHttpClientRequestExecutorWithParams("maxConnectionsPerRoute", paramValue)
            })
            assertThat exception.getMessage(), is("Max per route value may not be negative or zero")
        } else {
            def reqExec = buildHttpClientRequestExecutorWithParams("maxConnectionsPerRoute", paramValue)
            assertThat reqExec.getMaxConnectionPerRoute(), is(expectedValue)
        }
    }

    HttpClientRequestExecutor loadHttpClientRequestExecutor() {
        def e = new HttpClientRequestExecutor(new HttpClientConfiguration())
        return e
    }

    HttpClientRequestExecutor buildHttpClientRequestExecutorWithParams(String paramKey, String paramValue) {
        Map <String, String> requestExecutorParams = new HashMap<>()
        requestExecutorParams.put(paramKey, paramValue)

        def httpClientConfiguration = new HttpClientConfiguration()
        httpClientConfiguration.setRequestExecutorParams(requestExecutorParams)

        def httpClientRequestExecutor = new HttpClientRequestExecutor(httpClientConfiguration)
        return httpClientRequestExecutor
    }

    @DataProvider
    Object[][] validateAfterInactivity() {
        return standardConfigTests(2000)
    }

    @DataProvider
    Object[][] maxConnectionsPerRoute() {
        return standardConfigTests(Integer.MAX_VALUE/2 as int)
    }

    @DataProvider
    Object[][] maxConnections() {
        return standardConfigTests(Integer.MAX_VALUE)
    }

    @DataProvider
    Object[][] timeToLive() {
        return standardConfigTests(300000)
    }

    static Object[][] standardConfigTests(int defaultValue) {
        return [
               ["", defaultValue],
               ["0", 0],
               ["-1", -1],
               ["😊", defaultValue],
               ["O'Doyle Rules!", defaultValue],
               ["12", 12],
               [null, defaultValue]
        ]
    }

    @DataProvider
    Object[][] validateAfterInactivityParams() {
        return advancedConfigTests(2000)
    }

    @DataProvider
    Object[][] timeToLiveParams() {
        return advancedConfigTests(300000)
    }

    @DataProvider
    Object[][] maxConnectionsTotalParams() {
        return advancedConfigTests(Integer.MAX_VALUE)
    }

    @DataProvider
    Object[][] maxConnectionsPerRouteParams() {
        return advancedConfigTests(Integer.MAX_VALUE/2 as int)
    }

    static Object[][] advancedConfigTests(int defaultValue) {
        return [
            //valid params + valid sysprops
            ["2500", "3500", 2500],
            ["12", "3500",  12],
            ["0", "3500", 0],
            ["-1", "3500", -1],

            //invalid params + valid sysprops
            ["😊", "3500", 3500],
            ["Integer Value", "3500", 3500],
            [null, "3500", 3500],
            ["", "3500", 3500],
            ["3.14,15", "3500", 3500],
            ["3.14,15", "0", 0],
            ["3.14,15", "-1", -1],

            //invalid params + invalid sysprops
            [null, null, defaultValue],
            ["", null, defaultValue],
            ["", "", defaultValue],
            ["😊", "😁", defaultValue],
            ["3,1415", "612,12", defaultValue],
            ["Double Value", "Integer Value", defaultValue],

            //valid params + invalid sysprops
            ["2500", null, 2500],
            ["2501", "", 2501],
            ["2502", "😁", 2502],
            ["2503", "612,12", 2503],
            ["2504", "Integer Value", 2504]
        ]
    }

    static <T extends Throwable> T expect(Class<T> catchMe, Closure closure) {
        try {
            closure.call()
            Assert.fail("Expected ${catchMe.getName()} to be thrown.")
        } catch(e) {
            if (!e.class.isAssignableFrom(catchMe)) {
                throw e
            }
            return e
        }
    }

    @Override
    void run(IHookCallBack callBack, ITestResult testResult) {
        def originalProperties = System.getProperties()
        Properties copy = new Properties()
        copy.putAll(originalProperties)
        System.setProperties(copy)
        try {
            // run the tests
            if (!System.getProperty("java.version").startsWith("1.8")) {
                throw new SkipException("Test test only supported on JDK 8, see https://github.com/okta/okta-sdk-java/issues/276")
            }
            callBack.runTestMethod(testResult)

        } finally {
            System.setProperties(originalProperties)
        }
    }
}