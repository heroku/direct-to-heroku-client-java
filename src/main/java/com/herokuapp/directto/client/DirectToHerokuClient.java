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
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Ryan Brainard
 */
public class DirectToHerokuClient {

    public static final String STATUS = "status";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_IN_PROCESS = "inprocess";

    private long pollingIntervalInit = 1000;
    private double pollingIntervalMultiplier = 1.5;
    private long pollingTimeout = 10L * 60L * 1000L;

    private final WebResource baseResource;

    public DirectToHerokuClient(String apiKey) {
        this("https", "direct-to.herokuapp.com", 443, apiKey);
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
     *
     * @param pipelineName name of the pipeline to use
     * @param appName      app to which to deploy
     * @param files        bundle of files to deploy matching the pipeline
     * @return results from remote service
     * @throws DeploymentException if
     */
    public Map<String, String> deploy(String pipelineName, String appName, Map<String, File> files) throws DeploymentException {
        final WebResource deployRequest = baseResource.path("/direct/" + appName + "/" + pipelineName);

        final FormDataMultiPart form = new FormDataMultiPart();
        for (Map.Entry<String, File> file : files.entrySet()) {
            form.bodyPart(new FileDataBodyPart(file.getKey(), file.getValue()));
        }

        final ClientResponse deployResponse = deployRequest.type(MediaType.MULTIPART_FORM_DATA_TYPE).post(ClientResponse.class, form);
        if (HttpURLConnection.HTTP_ACCEPTED != deployResponse.getStatus()) {
            throw new DeploymentException("Deploy not accepted");
        }

        final List<String> locationHeaders = deployResponse.getHeaders().get("Location");
        if (locationHeaders == null || locationHeaders.get(0) == null) {
            throw new DeploymentException("Location header not found");
        }
        final String location = locationHeaders.get(0);

        final long startTime = System.currentTimeMillis();
        long pollingInterval = pollingIntervalInit;
        final WebResource pollingRequest = baseResource.path(location);
        Map response = deployResponse.getEntity(Map.class);
        while (STATUS_IN_PROCESS.equals(response.get(STATUS))) {
            response = pollingRequest.get(Map.class);

            if (System.currentTimeMillis() - startTime > pollingTimeout) {
                throw new DeploymentException("Polling timed out after " + pollingInterval + "ms");
            }

            try {
                Thread.sleep(pollingInterval *= pollingIntervalMultiplier);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (!STATUS_SUCCESS.equals(response.get(STATUS))) {
            final String unsuccessfulMsg = response.get("status") + ":" + response.get("message");
            throw new DeploymentException(unsuccessfulMsg, response.toString());
        }

        //noinspection unchecked
        return response;
    }

    public long getPollingIntervalInit() {
        return pollingIntervalInit;
    }

    public void setPollingIntervalInit(long pollingIntervalInit) {
        this.pollingIntervalInit = pollingIntervalInit;
    }

    public double getPollingIntervalMultiplier() {
        return pollingIntervalMultiplier;
    }

    public void setPollingIntervalMultiplier(double pollingIntervalMultiplier) {
        this.pollingIntervalMultiplier = pollingIntervalMultiplier;
    }

    public long getPollingTimeout() {
        return pollingTimeout;
    }

    public void setPollingTimeout(long pollingTimeout) {
        this.pollingTimeout = pollingTimeout;
    }
}
