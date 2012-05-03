package com.herokuapp.directto.client;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Ryan Brainard
 */
public class UserAgentFilter extends ClientFilter {

    private final String userAgent;

    public UserAgentFilter(String consumersUserAgent) {
        final String prefix = consumersUserAgent != null && !consumersUserAgent.trim().equals("") ? consumersUserAgent + " " : "";
        this.userAgent = prefix + getProjectUserAgent() + getJavaUserAgent();
    }

    private String getJavaUserAgent() {
        return " Java/" + System.getProperty("java.version");
    }

    @Override
    public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
        cr.getHeaders().add(HttpHeaders.USER_AGENT, userAgent);
        return getNext().handle(cr);
    }

    private static String getProjectUserAgent() {
        final Properties projectProperties = new Properties();
        try {
            projectProperties.load(UserAgentFilter.class.getClassLoader().getResourceAsStream("direct-to-heroku-client-java.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return projectProperties.get("project.artifactId") + "/" + projectProperties.get("project.version");
    }

}
