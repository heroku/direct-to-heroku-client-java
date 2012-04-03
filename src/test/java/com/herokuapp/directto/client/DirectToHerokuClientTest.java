package com.herokuapp.directto.client;

import com.herokuapp.directto.client.models.Pipeline;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Ryan Brainard
 */
public class DirectToHerokuClientTest {

    public static final String WAR_PIPELINE = "war";
    public static final String FATJAR_PIPELINE = "fatjar";

    private final String apiKey = System.getProperty("heroku.apiKey");
    private final String appName = System.getProperty("heroku.appName");
    private final String warFilePath = System.getProperty("heroku.warFile");
    private final DirectToHerokuClient client = new DirectToHerokuClient("http", "direct-to.herokuapp.com", 80, apiKey);

    @Before
    public void setUp() {

    }

    @Test
    public void testGetPipelineNames() throws Exception {
        final Collection<String> pipelineNames = client.getPipelineNames();
        assertTrue(pipelineNames.contains(WAR_PIPELINE));
        assertTrue(pipelineNames.contains(FATJAR_PIPELINE));
    }

    @Test
    public void testGetPipeline() throws Exception {
        final Pipeline pipeline = client.getPipeline(WAR_PIPELINE);
        assertEquals(WAR_PIPELINE, pipeline.getName());
        assertEquals("POST", pipeline.getCall().getMethod());
        assertEquals("/direct/<your app>/war", pipeline.getCall().getUrl());
        assertEquals("Directly deploy a war that will be executed with tomcat runner", pipeline.getManifest().getDescription());
        assertEquals("The war file to be deployed alongside tomcat runner", pipeline.getManifest().getRequiredFileInfo().get("war"));
    }

    @Test
    public void testDeploy() throws Exception {
        assertEquals("success", client.deploy(WAR_PIPELINE, appName, createWarBundle()).get("status"));
    }

    @Test
    public void testAsyncDeploy() throws Exception {
        assertEquals("success", client.deployAsync(WAR_PIPELINE, appName, createWarBundle()).get().get("status"));
    }

    @Test
    public void testVerify_Pass() throws Exception {
        client.verify(WAR_PIPELINE, appName, createWarBundle());
    }

    @Test
    public void testVerify_InvalidPipelineName() throws Exception {
        try {
            client.verify("BLAH", "anApp", null);
            fail();
        } catch (VerificationException e) {
            assertEquals("[Invalid pipeline name: BLAH]", e.getMessage());
        }
    }

    @Test
    public void testVerify_InvalidMisc() throws Exception {
        final HashMap<String, File> files = new HashMap<String, File>();
        files.put("meaningless", new File("i'm not really here"));
        try {
            client.verify("fatjar", "", files);
            fail();
        } catch (VerificationException e) {
            assertEquals("[App name must be populated, " +
                    "Required file not specified: jar (the fat jar), " +
                    "Required file not specified: procfile (The Procfile), " +
                    "File not found for: meaningless (i'm not really here)]",
                    e.getMessage());
        }
    }

    private Map<String, File> createWarBundle() {
        final File warFile = new File(warFilePath);
        assertTrue("Precondition", warFile.exists());

        final Map<String, File> files = new HashMap<String, File>(1);
        files.put("war", warFile);
        return files;
    }
}

