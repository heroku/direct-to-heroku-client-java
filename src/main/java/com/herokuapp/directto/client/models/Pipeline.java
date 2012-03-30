package com.herokuapp.directto.client.models;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Ryan Brainard
 */
@XmlRootElement
public class Pipeline {

    @JsonProperty
    private String name;
    @JsonProperty
    private Call call;
    @JsonProperty
    private Manifest manifest;

    public String getName() {
        return name;
    }

    public Call getCall() {
        return call;
    }

    public Manifest getManifest() {
        return manifest;
    }
}
