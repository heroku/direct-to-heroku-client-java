package com.herokuapp.directto.client.models;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Ryan Brainard
 */
@XmlRootElement
public class Call {

    public String method;
    public String url;

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }
}
