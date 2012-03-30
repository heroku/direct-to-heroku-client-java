package com.herokuapp.directto.client;

import com.heroku.api.HerokuAPI;

import java.util.concurrent.Callable;

import static junit.framework.Assert.assertEquals;

/**
 * @author Ryan Brainard
 */
class ReleasesAssertion {

    private final HerokuAPI api;
    private final String appName;
    private final int initialCount;

    public ReleasesAssertion(String apiKey, String appName) {
        this.api = new HerokuAPI(apiKey);
        this.appName = appName;
        this.initialCount = count();
    }

    public void assertIncrement(int by, Callable<Void> afterDoing) throws Exception {
        afterDoing.call();
        assertEquals(initialCount + by, count());
    }

    private int count() {
        return api.listReleases(appName).size();
    }
}
