package com.herokuapp.directto.client.models;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

/**
 * @author Ryan Brainard
 */
@XmlRootElement
public class Manifest {

    public String description;
    public Map<String, String> requiredFileInfo;

    public String getDescription() {
        return description;
    }

    public Map<String, String> getRequiredFileInfo() {
        return requiredFileInfo;
    }
}
