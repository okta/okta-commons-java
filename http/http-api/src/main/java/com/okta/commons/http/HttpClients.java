package com.okta.commons.http;

import com.okta.commons.lang.Classes;

public final class HttpClients {

    private HttpClients() {}

    public static RequestExecutor createRequestExecutor(HttpClientConfiguration clientConfiguration) {

        // TODO fix the jar name
        String msg = "Unable to find a '" + RequestExecutorFactory.class.getName() + "' " +
                "implementation on the classpath.  Please ensure you have added the " +
                "okta-sdk-httpclient.jar file to your runtime classpath.";
        return Classes.loadFromService(RequestExecutorFactory.class, msg).create(clientConfiguration);
    }
}
