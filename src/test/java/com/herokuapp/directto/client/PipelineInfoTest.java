package com.herokuapp.directto.client;

import com.herokuapp.directto.client.models.Pipeline;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ryan Brainard
 */
public class PipelineInfoTest extends DirectToHerokuClientBaseTest {

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
}

