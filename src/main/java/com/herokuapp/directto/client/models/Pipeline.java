package com.herokuapp.directto.client.models;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Ryan Brainard
 */
@XmlRootElement
public class Pipeline {

    public String name;
    public Call call;
    public Manifest manifest;

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
