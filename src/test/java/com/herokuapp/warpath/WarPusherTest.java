package com.herokuapp.warpath;

import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.Callable;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * @author Ryan Brainard
 */
public class WarPusherTest {

    public static final String API_KEY = System.getProperty("heroku.apiKey");
    public static final String APP_NAME = System.getProperty("heroku.appName");
    public static final File WAR_FILE = new File(ClassLoader.getSystemResource("sample-war.war").getFile());

    @Test
    public void testPush() throws Exception {
        new ReleasesAssertion(API_KEY, APP_NAME).assertIncrement(1, new Callable<Void>() {
            public Void call() throws Exception {
                new WarPusher(API_KEY).push(APP_NAME, WAR_FILE);
                return null;
            }
        });
    }

    @Test
    public void tesPush_BadAppName() throws Exception {
        new ReleasesAssertion(API_KEY, APP_NAME).assertIncrement(0, new Callable<Void>() {
            public Void call() throws Exception {
                try {
                    new WarPusher(API_KEY).push(APP_NAME + "garbage", WAR_FILE);
                    fail();
                } catch (RuntimeException e) {
                    assertTrue(e.getMessage().contains("not part of app"));
                }
                return null;
            }
        });
    }

    @Test
    public void testPush_BadApiKey() throws Exception {
        new ReleasesAssertion(API_KEY, APP_NAME).assertIncrement(0, new Callable<Void>() {
            public Void call() throws Exception {
                try {
                    new WarPusher("garbage").push(APP_NAME, WAR_FILE);
                    fail();
                } catch (RuntimeException e) {
                    assertTrue(e.getMessage().contains("Invalid username and password combination"));
                }
                return null;
            }
        });
    }

    @Test
    public void testPush_NonExistentFile() throws Exception {
        new ReleasesAssertion(API_KEY, APP_NAME).assertIncrement(0, new Callable<Void>() {
            public Void call() throws Exception {
                try {
                    new WarPusher(API_KEY).push(APP_NAME, new File("i/m/not/really/here"));
                    fail();
                } catch (FileNotFoundException e) {
                    // expected
                }
                return null;
            }
        });
    }
}