package com.herokuapp.directto.client.models;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Ryan Brainard
 */
public class Call {

    @JsonProperty
    private String method;
    @JsonProperty
    private String url;

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }
}
