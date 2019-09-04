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
package com.okta.commons.lang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.list;

public final class ApplicationInfo {

    // lookup okta libs by projects with a versions prop
    private static final String VERSION_FILE_LOCATION = "META-INF/okta/version.properties";

    //Integration Runtimes
    private static final String INTEGRATION_RUNTIME_SPRING_ID = "spring";
    private static final String INTEGRATION_RUNTIME_SPRING_CLASS = "org.springframework.context.ApplicationContext";

    //Rapid Prototyping
    private static final String RAPID_PROTOTYPING_SPRING_BOOT_ID = "spring-boot";
    private static final String RAPID_PROTOTYPING_SPRING_BOOT_CLASS = "org.springframework.boot.SpringApplication";

    //Runtime
    private static final String JAVA_SDK_RUNTIME_STRING = "java";

    ////Additional Information////

    //Security Frameworks
    private static final String SECURITY_FRAMEWORK_SHIRO_ID = "shiro";
    private static final String SECURITY_FRAMEWORK_SHIRO_CLASS = "org.apache.shiro.SecurityUtils";
    private static final String SECURITY_FRAMEWORK_SPRING_SECURITY_ID = "spring-security";
    private static final String SECURITY_FRAMEWORK_SPRING_SECURITY_CLASS = "org.springframework.security.core.SpringSecurityCoreVersion";

    //Web Servers
    private static final String WEB_SERVER_TOMCAT_ID = "tomcat";
    private static final String WEB_SERVER_TOMCAT_BOOTSTRAP_CLASS = "org.apache.catalina.startup.Bootstrap";
    private static final String WEB_SERVER_TOMCAT_EMBEDDED_CLASS = "org.apache.catalina.startup.Tomcat";
    private static final String WEB_SERVER_JETTY_ID = "jetty";
    private static final String WEB_SERVER_JETTY_CLASS = "org.eclipse.jetty.servlet.listener.ELContextCleaner";
    private static final String WEB_SERVER_JBOSS_ID = "jboss";
    private static final String WEB_SERVER_JBOSS_CLASS = "org.jboss.as.security.plugins.AuthenticationCacheEvictionListener";
    private static final String WEB_SERVER_WEBSPHERE_ID = "websphere";
    private static final String WEB_SERVER_WEBSPHERE_CLASS = "com.ibm.websphere.product.VersionInfo";
    private static final String WEB_SERVER_GLASSFISH_ID = "glassfish";
    private static final String WEB_SERVER_GLASSFISH_CLASS = "com.sun.enterprise.glassfish.bootstrap.GlassFishMain";
    private static final String WEB_SERVER_WEBLOGIC_ID = "weblogic";
    private static final String WEB_SERVER_WEBLOGIC_CLASS = "weblogic.version";
    private static final String WEB_SERVER_WILDFLY_ID = "wildfly";
    private static final String WEB_SERVER_WILDFLY_CLASS = "org.jboss.as.security.ModuleName";

    private static final String UNKNOWN_VERSION = "unknown";
    private static final Logger log = LoggerFactory.getLogger(ApplicationInfo.class);

    //Placeholder for the actual env info map
    private static final Map<String, String> NAME_VERSION_MAP = createNameToVersionMap();

    private ApplicationInfo() {}

    public static Map<String, String> get() {
        return NAME_VERSION_MAP;
    }

    private static Map<String, String> createNameToVersionMap() {

        List<NameAndVersion> nameAndVersions = new ArrayList<>(oktaComponentsFromVersionMetadata());
        nameAndVersions.add(getShiroInfo());                // shiro
        nameAndVersions.add(getSpringFrameworkInfo());      // spring
        nameAndVersions.add(getSpringBootInfo());           // spring-boot
        nameAndVersions.add(getSpringSecurityInfo());       // spring-security
        nameAndVersions.add(getWebServerInfo());            // tomcat | jetty | jboss | websphere | glassfish | weblogic | wildfly
        nameAndVersions.add(getJavaSDKRuntimeInfo());       // java
        nameAndVersions.add(getOSInfo());                   // Mac OS X
        return nameAndVersions.stream()
                .filter(Objects::nonNull)
                .collect(LinkedHashMap::new, // keep order
                        (map, info) -> map.put(info.name, info.version),
                        Map::putAll);
    }

    private static NameAndVersion getSpringFrameworkInfo() {
        return getFullEntryStringUsingManifest(INTEGRATION_RUNTIME_SPRING_CLASS, INTEGRATION_RUNTIME_SPRING_ID);
    }

    private static NameAndVersion getJavaSDKRuntimeInfo() {
        return new NameAndVersion(JAVA_SDK_RUNTIME_STRING, System.getProperty("java.version"));
    }

    private static NameAndVersion getOSInfo() {
        return new NameAndVersion(System.getProperty("os.name"),System.getProperty("os.version"));
    }

    //Spring Boot
    private static NameAndVersion getSpringBootInfo() {
        return getFullEntryStringUsingManifest(RAPID_PROTOTYPING_SPRING_BOOT_CLASS, RAPID_PROTOTYPING_SPRING_BOOT_ID);
    }

    private static NameAndVersion getShiroInfo() {
        return getFullEntryStringUsingManifest(SECURITY_FRAMEWORK_SHIRO_CLASS, SECURITY_FRAMEWORK_SHIRO_ID);
    }

    private static NameAndVersion getSpringSecurityInfo() {
        return getFullEntryStringUsingManifest(SECURITY_FRAMEWORK_SPRING_SECURITY_CLASS, SECURITY_FRAMEWORK_SPRING_SECURITY_ID);
    }

    private static NameAndVersion getWebServerInfo() {
        NameAndVersion webServerInfo;
        //Glassfish uses Tomcat internally, therefore the Glassfish check must be carried out before Tomcat's
        webServerInfo = getFullEntryStringUsingManifest(WEB_SERVER_GLASSFISH_CLASS, WEB_SERVER_GLASSFISH_ID);
        if(webServerInfo != null) {
            return webServerInfo;
        }
        webServerInfo = getFullEntryStringUsingManifest(WEB_SERVER_TOMCAT_BOOTSTRAP_CLASS, WEB_SERVER_TOMCAT_ID);
        if(webServerInfo != null) {
            return webServerInfo;
        }
        webServerInfo = getFullEntryStringUsingManifest(WEB_SERVER_TOMCAT_EMBEDDED_CLASS, WEB_SERVER_TOMCAT_ID);
        if(webServerInfo != null) {
            return webServerInfo;
        }
        webServerInfo = getFullEntryStringUsingManifest(WEB_SERVER_JETTY_CLASS, WEB_SERVER_JETTY_ID);
        if(webServerInfo != null) {
            return webServerInfo;
        }
        //WildFly must be before JBoss
        webServerInfo = getWildFlyEntryInfo();
        if(webServerInfo != null) {
            return webServerInfo;
        }
        webServerInfo = getFullEntryStringUsingManifest(WEB_SERVER_JBOSS_CLASS, WEB_SERVER_JBOSS_ID);
        if(webServerInfo != null) {
            return webServerInfo;
        }
        webServerInfo = getWebSphereEntryInfo();
        if(webServerInfo != null) {
            return webServerInfo;
        }
        webServerInfo = getWebLogicEntryInfo();
        if(webServerInfo != null) {
            return webServerInfo;
        }

        return null;
    }

    private static NameAndVersion getFullEntryStringUsingManifest(String fqcn, String entryId) {
        if (Classes.isAvailable(fqcn)) {
            return new NameAndVersion(entryId, getVersionInfoInManifest(fqcn));
        }
        return null;
    }

    private static NameAndVersion getWebSphereEntryInfo() {
        if (Classes.isAvailable(WEB_SERVER_WEBSPHERE_CLASS)) {
            return new NameAndVersion(WEB_SERVER_WEBSPHERE_ID, getWebSphereVersion());
        }
        return null;
    }

    private static NameAndVersion getWebLogicEntryInfo() {
        if (Classes.isAvailable(WEB_SERVER_WEBLOGIC_CLASS)) {
            return new NameAndVersion(WEB_SERVER_WEBLOGIC_ID, getWebLogicVersion());
        }
        return null;
    }

    private static NameAndVersion getWildFlyEntryInfo() {
        try {
            if (Classes.isAvailable(WEB_SERVER_WILDFLY_CLASS)) {
                Package wildFlyPkg = Classes.forName(WEB_SERVER_WILDFLY_CLASS).getPackage();
                if (wildFlyPkg != null
                    && Strings.hasText(wildFlyPkg.getImplementationTitle()) && wildFlyPkg.getImplementationTitle().contains("WildFly")) {
                        return new NameAndVersion(WEB_SERVER_WILDFLY_ID, wildFlyPkg.getImplementationVersion());
                }
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e){ //NOPMD
            //there was a problem obtaining the WildFly version
        }
        return null;
    }

    /**
     * WARNING: This method must never be invoked unless we already know that the class identified by the parameter {@code fqcn}
     * really exists in the classpath. For example, we first need to assure that {@code Classes.isAvailable(fqcn))} is <code>TRUE</code>
     */
    private static String getVersionInfoInManifest(String fqcn) {
        String version = null;
        //get class package
        Package thePackage = Classes.forName(fqcn).getPackage();
        // package could be null in some uberjars
        if (thePackage != null) {
            //examine the package object
            version = thePackage.getSpecificationVersion();
            if (!Strings.hasText(version)) {
                version = thePackage.getImplementationVersion();
            }
        }
        if(!Strings.hasText(version)) {
            version = UNKNOWN_VERSION;
        }
        return version;
    }

    /**
     * This method should only be invoked after already knowing that the class identified by {@code WEB_SERVER_WEBSPHERE_CLASS}
     * really exists in the classpath. For example, it can be checked that {@code Classes.isAvailable(WEB_SERVER_WEBSPHERE_CLASS))}
     * is {@code TRUE}
     */
    private static String getWebSphereVersion() {
        try {
            Class<?> versionClass = Class.forName(WEB_SERVER_WEBSPHERE_CLASS);
            Object versionInfo = versionClass.newInstance();
            Method method = versionClass.getDeclaredMethod("runReport", String.class, PrintWriter.class);
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            method.invoke(versionInfo, "", printWriter);
            String version = stringWriter.toString();
            // version looks like this, so we need to "substring" it:
            //
            //
            //IBM WebSphere Product Installation Status Report
            //--------------------------------------------------------------------------------
            //
            //Report at date and time August 13, 2014 1:12:06 PM ART
            //
            //Installation
            //--------------------------------------------------------------------------------
            //Product Directory        C:\Program Files\IBM\WebSphere\AppServer
            //Version Directory        C:\Program Files\IBM\WebSphere\AppServer\properties\version
            //DTD Directory            C:\Program Files\IBM\WebSphere\AppServer\properties\version\dtd
            //Log Directory            C:\Documents and Settings\All Users\Application Data\IBM\Installation Manager\logs
            //
            //Product List
            //--------------------------------------------------------------------------------
            //BASETRIAL                installed
            //
            //Installed Product
            //--------------------------------------------------------------------------------
            //Name                  IBM WebSphere Application Server
            //Version               8.5.5.2
            //ID                    BASETRIAL
            //Build Level           cf021414.01
            //Build Date            4/8/14
            //Package               com.ibm.websphere.BASETRIAL.v85_8.5.5002.20140408_1947
            //Architecture          x86 (32 bit)
            //Installed Features    IBM 32-bit WebSphere SDK for Java
            //WebSphere Application Server Full Profile

            version = version.substring(version.indexOf("Installed Product"));
            version = version.substring(version.indexOf("Version"));
            version = version.substring(version.indexOf(" "), version.indexOf("\n")).trim();
            return version;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) { //NOPMD
            //there was a problem obtaining the WebSphere version
        }
        //returning 'unknown' so we can identify in the User-Agent String that we are not properly handling some WebSphere version
        return UNKNOWN_VERSION;
    }

    /**
     * This method should only be invoked after already knowing that the class identified by {@code WEB_SERVER_WEBLOGIC_CLASS}
     * really exists in the classpath. For example, it can be checked that {@code Classes.isAvailable(WEB_SERVER_WEBLOGIC_CLASS))}
     * is {@code TRUE}
     */
    private static String getWebLogicVersion() {
        try {
            Class<?> versionClass = Class.forName(WEB_SERVER_WEBLOGIC_CLASS);
            Object version = versionClass.newInstance();
            Method method = versionClass.getDeclaredMethod("getReleaseBuildVersion");
            return (String) method.invoke(version);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) { //NOPMD
            //there was a problem obtaining the WebLogic version
        }
        //returning 'unknown' so we can identify in the User-Agent String that we are not properly handling some WebLogic version
        return UNKNOWN_VERSION;
    }

    private static Set<NameAndVersion> oktaComponentsFromVersionMetadata() {
        Set<NameAndVersion> results = new HashSet<>();
        try {
            list(ApplicationInfo.class.getClassLoader().getResources(VERSION_FILE_LOCATION)).stream()
                    .map(ApplicationInfo::loadProps)
                    .forEach(properties -> results.addAll(entriesFromOktaVersionMetadata(properties)));
        } catch (IOException e) { //NOPMD
            // don't fail when gathering info
            log.warn("Failed to locate okta component version metadata as a resource: {}", VERSION_FILE_LOCATION);
        }
        return results;
    }

    private static Set<NameAndVersion> entriesFromOktaVersionMetadata(Properties properties) {

        if (properties == null) {
            return Collections.emptySet();
        }

        return properties.entrySet().stream()
            .map(entry -> new NameAndVersion((String) entry.getKey(), (String) entry.getValue()))
            .collect(Collectors.toSet());
    }

    private static Properties loadProps(URL resourceUrl) {
        try {
            Properties props = new Properties();
            props.load(resourceUrl.openStream());
            return props;
        } catch (IOException e) {
            // don't fail when gathering info
            log.warn("Failed to open properties file: '{}', but this file was detected on your classpath", resourceUrl);
        }
        return null;
    }

    private static class NameAndVersion {

        private final String name;
        private final String version;

        private NameAndVersion(String name, String version) {
            this.name = name;
            this.version = version;
        }
    }
}