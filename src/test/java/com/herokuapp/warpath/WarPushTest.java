package com.herokuapp.warpath;

import com.heroku.api.HerokuAPI;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.Callable;

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
        new ReleaseCountAssertion(API_KEY, APP_NAME).assertIncrement(1, new Callable<Void>() {
            public Void call() throws Exception {
                new WarPathClient(API_KEY).push(APP_NAME, WAR_FILE);
                return null;
            }
        });
    }

    @Test
    public void testNonExistentFile() throws Exception {
        new ReleaseCountAssertion(API_KEY, APP_NAME).assertIncrement(0, new Callable<Void>() {
            public Void call() throws Exception {
                try {
                    new WarPathClient(API_KEY).push(APP_NAME, new File("i/m/not/really/here"));
                    fail();
                } catch (FileNotFoundException e) {
                    // expected
                }
                return null;
            }
        });
    }

    private static class ReleaseCountAssertion {
        private final HerokuAPI api;
        private final String appName;
        private final int initialCount;

        ReleaseCountAssertion(String apiKey, String appName) {
            this.api = new HerokuAPI(apiKey);
            this.appName = appName;
            this.initialCount = count();
        }

        void assertIncrement(int by, Callable<Void> afterDoing) throws Exception {
            afterDoing.call();
            assertEquals(initialCount + by, count());
        }

        private int count() {
            return api.listReleases(appName).size();
        }
    }
}