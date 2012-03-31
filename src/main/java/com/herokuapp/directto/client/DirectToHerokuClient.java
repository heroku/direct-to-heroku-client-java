package com.herokuapp.directto.client;

import com.herokuapp.directto.client.models.Pipeline;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Ryan Brainard
 */
public class DirectToHerokuClient {

    private final WebResource baseResource;

    public DirectToHerokuClient(String apiKey) {
        this("http", "direct-to.herokuapp.com", 80, apiKey);
    }

    DirectToHerokuClient(String scheme, String host, int port, String apiKey) {
        final ClientConfig config = new DefaultClientConfig();
        config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

        final Client jerseyClient = Client.create(config);
        jerseyClient.addFilter(new HTTPBasicAuthFilter("", apiKey));

        baseResource = jerseyClient.resource(scheme + "://" + host + ":" + port);
    }

    /**
     * Provides a list of all possible pipelines
     */
    public Collection<String> getPipelineNames() {
        //noinspection unchecked
        return baseResource.path("/pipelines").get(Map.class).keySet();
    }

    /**
     * Provides details about a pipeline
     */
    public Pipeline getPipeline(String pipelineName) {
        return baseResource.path("/pipelines/" + pipelineName).get(Pipeline.class);
    }

    /**
     * Client side, pre-deployment verification of payload for deployment with a pipeline
     *
     * @throws VerificationException when a verification issue is found
     */
    public void verify(String pipelineName, String appName, Map<String, File> files) throws VerificationException {
        final VerificationException.Aggregator problems = new VerificationException.Aggregator();

        if (appName == null || appName.trim().equals("")) {
            problems.addMessage("App name must be populated");
        }

        Pipeline pipeline = null;
        try {
            pipeline = getPipeline(pipelineName);
        } catch (UniformInterfaceException e) {
            problems.addMessage("Invalid pipeline name: " + pipelineName).detonate();
        }

        for (Map.Entry<String, String> requiredFile : pipeline.getManifest().getRequiredFileInfo().entrySet()) {
            if (!files.containsKey(requiredFile.getKey())) {
                problems.addMessage("Required file not specified: " + requiredFile.getKey() + " (" + requiredFile.getValue() + ")");
            }
        }

        for (Map.Entry<String, File> file : files.entrySet()) {
            if (file.getValue() == null || !file.getValue().exists()) {
                problems.addMessage("File not found for: " + file.getKey() + " (" + file.getValue() + ")");
            }
        }

        problems.detonate();
    }

    /**
     * Deploys a bundle of files to an app name using a given pipeline.
     * <p/>
     * Consider calling {@link #verify} prior to calling this method for client-side verification.
     * <p/>
     * This call blocks until the remote job is no longer in progress without any timeout.
     * If you want to enforce a timeout, use {@link #deployAsync} and call
     * {@link Future#get(long, java.util.concurrent.TimeUnit)}.
     *
     * @param pipelineName name of the pipeline to use
     * @param appName      app to which to deploy
     * @param files        bundle of files to deploy matching the pipeline
     * @return results from remote service
     */
    public Map<String, String> deploy(String pipelineName, String appName, Map<String, File> files) {
        final WebResource deployRequest = baseResource.path("/direct/" + appName + "/" + pipelineName);

        final FormDataMultiPart form = new FormDataMultiPart();
        for (Map.Entry<String, File> file : files.entrySet()) {
            form.bodyPart(new FileDataBodyPart(file.getKey(), file.getValue()));
        }

        final ClientResponse deployResponse = deployRequest.type(MediaType.MULTIPART_FORM_DATA_TYPE).post(ClientResponse.class, form);

        final WebResource pollingRequest = baseResource.path(deployResponse.getHeaders().get("Location").get(0));
        Map response = deployResponse.getEntity(Map.class);
        long pollingInterval = 1000L;
        while (response.get("status").equals("inprocess")) {
            response = pollingRequest.get(Map.class);
            try {
                Thread.sleep(pollingInterval *= 1.5);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        //noinspection unchecked
        return response;
    }

    /**
     * Same as @{#deploy}, but launches deployment in a separate thread and returns immediately with a {@link Future} result.
     */
    public Future<Map<String, String>> deployAsync(final String pipelineName, final String appName, final Map<String, File> files) {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            return executorService.submit(new Callable<Map<String, String>>() {
                public Map<String, String> call() throws Exception {
                    return deploy(pipelineName, appName, files);
                }
            });
        } finally {
            executorService.shutdown();
        }
    }
}
