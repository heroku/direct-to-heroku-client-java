package com.herokuapp.directto.client;

import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import static com.herokuapp.directto.client.DirectToHerokuClient.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Ryan Brainard
 */
public class DeploymentTest extends DirectToHerokuClientBaseTest {

    @Test
    public void testDeploy() throws Exception {
        assertEquals(STATUS_SUCCESS, client.deploy(WAR_PIPELINE, appName, warBundle).get(STATUS));
    }

    @Test
    public void testDeploy_NoApiKeySet() throws Exception {
        final DirectToHerokuClient clientWithNoApiKeySet = new Builder().setApiKey("").build();

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
        final DirectToHerokuClient badClient = new Builder().setScheme("http").setHost("example.com").setPort(80).setApiKey(apiKey).build();

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
        DirectToHerokuClient clientWithShortTimeout = new Builder().setApiKey(apiKey).build();

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

}

