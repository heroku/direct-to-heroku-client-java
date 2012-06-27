package com.herokuapp.directto.client;

import java.io.File;
import java.util.Map;

/**
 * @author Ryan Brainard
 */
public final class DeployRequest {

    public static final int DEFAULT_POLLING_INTERVAL_INIT = 1000;
    public static final double DEFAULT_POLLING_INTERVAL_MULTIPLIER = 1.5;
    public static final long DEFAULT_POLLING_TIMEOUT = 10L * 60L * 1000L;

    private final String pipelineName;
    private final String appName;
    private final Map<String, File> files;
    private EventSubscription eventSubscription = new EventSubscription();
    private long pollingIntervalInit = DEFAULT_POLLING_INTERVAL_INIT;
    private double pollingIntervalMultiplier = DEFAULT_POLLING_INTERVAL_MULTIPLIER;
    private long pollingTimeout = DEFAULT_POLLING_TIMEOUT;

    public DeployRequest(String pipelineName, String appName, Map<String, File> files) {
        this.pipelineName = pipelineName;
        this.appName = appName;
        this.files = files;
    }

    public DeployRequest setEventSubscription(EventSubscription eventSubscription) {
        this.eventSubscription = eventSubscription;
        return this;
    }

    public DeployRequest setPollingIntervalInit(long pollingIntervalInit) {
        this.pollingIntervalInit = pollingIntervalInit;
        return this;
    }

    public DeployRequest setPollingIntervalMultiplier(double pollingIntervalMultiplier) {
        this.pollingIntervalMultiplier = pollingIntervalMultiplier;
        return this;
    }

    public DeployRequest setPollingTimeout(long pollingTimeout) {
        this.pollingTimeout = pollingTimeout;
        return this;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getAppName() {
        return appName;
    }

    public Map<String, File> getFiles() {
        return files;
    }

    public EventSubscription getEventSubscription() {
        return eventSubscription;
    }

    public long getPollingIntervalInit() {
        return pollingIntervalInit;
    }

    public double getPollingIntervalMultiplier() {
        return pollingIntervalMultiplier;
    }

    public long getPollingTimeout() {
        return pollingTimeout;
    }

}
