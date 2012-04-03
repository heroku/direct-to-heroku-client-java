package com.herokuapp.directto.client;

import com.herokuapp.directto.client.models.Pipeline;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.util.*;

import static com.herokuapp.directto.client.DirectToHerokuClient.STATUS_SUCCESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ryan Brainard
 */
public class DirectToHerokuClientTest {

    public static final String WAR_PIPELINE = "war";
    public static final String FATJAR_PIPELINE = "fatjar";

    private final String apiKey = System.getProperty("heroku.apiKey");
    private final String appName = System.getProperty("heroku.appName");
    private final String warFilePath = System.getProperty("heroku.warFile");
    private final Map<String, File> warBundle = createWarBundle(warFilePath);
    private final DirectToHerokuClient client = new DirectToHerokuClient("http", "direct-to.herokuapp.com", 80, apiKey);

    @Rule
    public final ExpectedException exceptionRule = ExpectedException.none();

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
        assertEquals(STATUS_SUCCESS, client.deploy(WAR_PIPELINE, appName, warBundle).get(STATUS_SUCCESS));
    }

    @Test
    public void testAsyncDeploy() throws Exception {
        assertEquals(STATUS_SUCCESS, client.deployAsync(WAR_PIPELINE, appName, warBundle).get().get(STATUS_SUCCESS));
    }

    @Test
    public void testDeploy_NoAccessToApp() throws Exception {
        exceptionRule.expect(DeploymentException.class);
        exceptionRule.expectMessage("not part of app");
        client.deploy(WAR_PIPELINE, UUID.randomUUID().toString(), warBundle);
    }

    @Test
    public void testDeploy_BadResponse() throws Exception {
        final DirectToHerokuClient badClient = new DirectToHerokuClient("http", "example.com", 80, apiKey);

        exceptionRule.expect(DeploymentException.class);
        exceptionRule.expectMessage("Deploy not accepted");
        badClient.deploy(WAR_PIPELINE, appName, warBundle);
    }

    @Test
    public void testVerify_Pass() throws Exception {
        client.verify(WAR_PIPELINE, appName, warBundle);
    }

    @Test
    public void testVerify_InvalidPipelineName() throws Exception {
        exceptionRule.expect(VerificationException.class);
        exceptionRule.expectMessage("[Invalid pipeline name");
        client.verify("BAD_PIPELINE_NAME", "anApp", createWarBundle(warFilePath));
    }

    @Test
    public void testVerify_InvalidMisc() throws Exception {
        final HashMap<String, File> files = new HashMap<String, File>();
        files.put("meaningless", new File("i'm not really here"));

        exceptionRule.expect(VerificationException.class);
        exceptionRule.expectMessage("App name must be populated");
        exceptionRule.expectMessage("Required file not specified: jar (the fat jar)");
        exceptionRule.expectMessage("Required file not specified: procfile (The Procfile)");
        exceptionRule.expectMessage("File not found for: meaningless (i'm not really here)");
        client.verify("fatjar", "", files);
    }

    private Map<String, File> createWarBundle(String warFilePath) {
        final File warFile = new File(warFilePath);
        assertTrue("Precondition", warFile.exists());

        final Map<String, File> files = new HashMap<String, File>(1);
        files.put("war", warFile);

        return Collections.unmodifiableMap(files);
    }
}

