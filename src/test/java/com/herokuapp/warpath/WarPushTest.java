package com.herokuapp.warpath;

import com.heroku.api.HerokuAPI;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * @author Ryan Brainard
 */
public class WarPushTest {

    public static final String API_KEY = System.getProperty("heroku.apiKey");
    public static final String APP_NAME = System.getProperty("heroku.appName");
    public static final File WAR_FILE = new File(ClassLoader.getSystemResource("sample-war.war").getFile());

    @Test
    public void testWarPush() throws Exception {
        final ReleaseCounter releaseCounter = new ReleaseCounter(API_KEY, APP_NAME);
        final int releasesBeforePush = releaseCounter.count();

        new WarPathClient(API_KEY).push(APP_NAME, WAR_FILE);

        assertEquals(releasesBeforePush + 1, releaseCounter.count());
    }

    @Test
    public void testNonExistentFile() throws Exception {
        final ReleaseCounter releaseCounter = new ReleaseCounter(API_KEY, APP_NAME);
        final int releasesBeforePush = releaseCounter.count();

        try {
            new WarPathClient(API_KEY).push(APP_NAME, new File("i/m/not/really/here"));
            fail();
        } catch (FileNotFoundException e) {
            // expected
        }

        assertEquals(releasesBeforePush, releaseCounter.count());
    }


    private static class ReleaseCounter {
        private final String appName;
        private final HerokuAPI api;

        ReleaseCounter(String apiKey, String appName) {
            this.api = new HerokuAPI(apiKey);
            this.appName = appName;
        }

        int count() {
            return api.listReleases(appName).size();
        }
    }
}
