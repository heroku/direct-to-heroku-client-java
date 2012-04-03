package com.herokuapp.directto.client;

/**
 * @author Ryan Brainard
 */
public class DeploymentException extends RuntimeException {

    private final String details;

    public DeploymentException(String msg) {
        super(msg);
        details = null;
    }

    public DeploymentException(String msg, String details) {
        super(msg);
        this.details = details;
    }

    public String getDetails() {
        return details;
    }
}
