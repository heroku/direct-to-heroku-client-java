package com.herokuapp.directto.client;

import com.herokuapp.directto.client.models.Pipeline;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.util.*;

import static com.herokuapp.directto.client.DirectToHerokuClient.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ryan Brainard
 */
public class DirectToHerokuClientTest {

    public static final String WAR_PIPELINE = "war";
    public static final String TARGZ_PIPELINE = "targz";
    public static final String FATJAR_PIPELINE = "fatjar";

    private final String apiKey = getSystemPropertyOrThrow("heroku.apiKey");
    private final String appName = getSystemPropertyOrThrow("heroku.appName");
    private final Map<String, File> warBundle = createWarBundle(ClassLoader.getSystemResource("sample-war.war").getPath());

    private final DirectToHerokuClient client = new DirectToHerokuClient(apiKey);
    @Rule
    public final ExpectedException exceptions = ExpectedException.none();

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
        assertEquals(STATUS_SUCCESS, client.deploy(WAR_PIPELINE, appName, warBundle).get(STATUS));
    }

    @Test
    public void testDeploy_NoApiKeySet() throws Exception {
        final DirectToHerokuClient clientWithNoApiKeySet = new DirectToHerokuClient("");

        exceptions.expect(DeploymentException.class);
        exceptions.expectMessage("Unable to get user info");
        clientWithNoApiKeySet.deploy(WAR_PIPELINE, appName, warBundle);
    }

    @Test
    public void testDeploy_NoAccessToApp() throws Exception {
        exceptions.expect(DeploymentException.class);
        exceptions.expectMessage("not part of app");
        client.deploy(WAR_PIPELINE, UUID.randomUUID().toString(), warBundle);
    }

    @Test
    public void testDeploy_BadResponse() throws Exception {
        final DirectToHerokuClient badClient = new DirectToHerokuClient("http", "example.com", 80, apiKey);

        exceptions.expect(DeploymentException.class);
        exceptions.expectMessage("Deploy not accepted");
        badClient.deploy(WAR_PIPELINE, appName, warBundle);
    }

    @Test
    public void testDeploy_InvalidRequiredFiles() throws Exception {
        exceptions.expect(DeploymentException.class);
        exceptions.expectMessage("fatjar requires the following file params:jar, procfile");
        client.deploy(FATJAR_PIPELINE, appName, warBundle);
    }

    @Test
    public void testDeploy_WithoutTimeout() throws Exception {
        DirectToHerokuClient clientWithShortTimeout = new DirectToHerokuClient(apiKey);

        exceptions.expect(DeploymentException.class);
        exceptions.expectMessage("Polling timed out");
        clientWithShortTimeout.deploy(WAR_PIPELINE, appName, warBundle, DEFAULT_POLLING_INTERVAL_INIT, DEFAULT_POLLING_INTERVAL_MULTIPLIER, 1);
    }

    @Test
    public void testVerify_Pass() throws Exception {
        client.verify(WAR_PIPELINE, appName, warBundle);
    }

    @Test
    public void testVerify_InvalidPipelineName() throws Exception {
        exceptions.expect(VerificationException.class);
        exceptions.expectMessage("[Invalid pipeline name");
        client.verify("BAD_PIPELINE_NAME", "anApp", warBundle);
    }

    @Test
    public void testVerify_Targz_NonEmptyProcfile() throws Exception {
        final HashMap<String, File> files = new HashMap<String, File>();
        files.put("targz", File.createTempFile("some", "targz"));
        files.put("procfile", new File(ClassLoader.getSystemResource("Procfile").getPath()));

        client.verify(TARGZ_PIPELINE, appName, files);
    }

    @Test
    public void testVerify_Targz_EmptyProcfile() throws Exception {
        final HashMap<String, File> files = new HashMap<String, File>();
        files.put("targz", File.createTempFile("some", "targz"));
        files.put("procfile", File.createTempFile("empty", "Procfile"));

        exceptions.expect(VerificationException.class);
        exceptions.expectMessage("Procfile must not be empty");
        client.verify(TARGZ_PIPELINE, appName, files);
    }

    @Test
    public void testVerify_TwoArtifactsWithSameName() throws Exception {
        final File sameFile = File.createTempFile("same", "name");
        final HashMap<String, File> files = new HashMap<String, File>();
        files.put("targz", sameFile);
        files.put("procfile", sameFile);

        exceptions.expect(VerificationException.class);
        exceptions.expectMessage("All files must be unique");
        client.verify(TARGZ_PIPELINE, appName, files);
    }

    @Test
    public void testVerify_TwoArtifactsWithDifferentNames() throws Exception {
        final HashMap<String, File> files = new HashMap<String, File>();
        files.put("targz", File.createTempFile("some", "targz"));
        files.put("procfile", new File(ClassLoader.getSystemResource("Procfile").getPath()));

        client.verify(TARGZ_PIPELINE, appName, files);
    }

    @Test
    public void testVerify_InvalidMisc() throws Exception {
        final HashMap<String, File> files = new HashMap<String, File>();
        files.put("meaningless", new File("i'm not really here"));

        exceptions.expect(VerificationException.class);
        exceptions.expectMessage("App name must be populated");
        exceptions.expectMessage("Required file not specified: jar (the fat jar)");
        exceptions.expectMessage("Required file not specified: procfile (The Procfile)");
        exceptions.expectMessage("File not found for: meaningless (i'm not really here)");
        client.verify(FATJAR_PIPELINE, "", files);
    }

    private Map<String, File> createWarBundle(String warFilePath) {
        final File warFile = new File(warFilePath);
        assertTrue("Precondition", warFile.exists());

        final Map<String, File> files = new HashMap<String, File>(1);
        files.put("war", warFile);

        return Collections.unmodifiableMap(files);
    }

    private String getSystemPropertyOrThrow(String key) {
        if (System.getProperty(key) != null) {
            return System.getProperty(key);
        } else {
            throw new IllegalStateException("System property [" + key + "] not set. Be sure to set properties when running tests.");
        }
    }
}

