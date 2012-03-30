package com.herokuapp.directto.client.models;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Map;

/**
 * @author Ryan Brainard
 */
public class Manifest {

    @JsonProperty
    private String description;
    @JsonProperty
    private Map<String, String> requiredFileInfo;

    public String getDescription() {
        return description;
    }

    public Map<String, String> getRequiredFileInfo() {
        return requiredFileInfo;
    }
}
