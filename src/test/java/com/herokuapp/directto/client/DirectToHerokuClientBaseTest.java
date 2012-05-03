package com.herokuapp.directto.client;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * @author Ryan Brainard
 */
public abstract class DirectToHerokuClientBaseTest {

    protected static final String WAR_PIPELINE = "war";
    protected static final String TARGZ_PIPELINE = "targz";
    protected static final String FATJAR_PIPELINE = "fatjar";

    protected final String apiKey = getSystemPropertyOrThrow("heroku.apiKey");
    protected final String appName = getSystemPropertyOrThrow("heroku.appName");
    protected final Map<String, File> warBundle = createWarBundle(ClassLoader.getSystemResource("sample-war.war").getPath());

    protected final DirectToHerokuClient client = new DirectToHerokuClient.Builder().setApiKey(apiKey).build();
    @Rule
    public final ExpectedException exceptions = ExpectedException.none();

    protected Map<String, File> createWarBundle(String warFilePath) {
        final File warFile = new File(warFilePath);
        assertTrue("Precondition", warFile.exists());

        final Map<String, File> files = new HashMap<String, File>(1);
        files.put("war", warFile);

        return Collections.unmodifiableMap(files);
    }

    protected String getSystemPropertyOrThrow(String key) {
        if (System.getProperty(key) != null) {
            return System.getProperty(key);
        } else {
            throw new IllegalStateException("System property [" + key + "] not set. Be sure to set properties when running tests.");
        }
    }

}
