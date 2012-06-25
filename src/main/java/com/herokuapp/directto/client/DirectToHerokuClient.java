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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @author Ryan Brainard
 */
public class DirectToHerokuClient {

    public static final String DEFAULT_SCHEME = "https";
    public static final String DEFAULT_HOST = "direct-to.herokuapp.com";
    public static final int DEFAULT_PORT = 443;
    public static final int DEFAULT_POLLING_INTERVAL_INIT = 1000;
    public static final double DEFAULT_POLLING_INTERVAL_MULTIPLIER = 1.5;
    public static final long DEFAULT_POLLING_TIMEOUT = 10L * 60L * 1000L;

    public static final String STATUS = "status";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_IN_PROCESS = "inprocess";

    private final WebResource baseResource;

    private DirectToHerokuClient(Builder builder) {
        final ClientConfig config = new DefaultClientConfig();
        config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        config.getProperties().put(ClientConfig.PROPERTY_CHUNKED_ENCODING_SIZE, -1 /* default chunk size */);

        final Client jerseyClient = Client.create(config);
        jerseyClient.addFilter(new HTTPBasicAuthFilter("", builder.apiKey));
        jerseyClient.addFilter(new UserAgentFilter(builder.consumersUserAgent));

        baseResource = jerseyClient.resource(builder.scheme + "://" + builder.host + ":" + builder.port);
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

        if (new HashSet<File>(files.values()).size() != files.size()) {
            problems.addMessage("All files must be unique");
        }

        if (files.containsKey("procfile")) {
            InputStream in = null;
            try {
                in = new FileInputStream(files.get("procfile"));
                if (in.read() == -1) {
                    problems.addMessage("Procfile must not be empty");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (in != null) in.close();
                } catch (IOException e) {
                    // swallow
                }
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
    public Map<String, String> deploy(String pipelineName, String appName, Map<String, File> files) {
        return deploy(pipelineName, appName, files, DEFAULT_POLLING_INTERVAL_INIT, DEFAULT_POLLING_INTERVAL_MULTIPLIER, DEFAULT_POLLING_TIMEOUT);
    }

    public Map<String, String> deploy(String pipelineName, String appName, Map<String, File> files, long pollingIntervalInit, double pollingIntervalMultiplier, long pollingTimeout) throws DeploymentException {
        return poll(upload(pipelineName, appName, files), pollingIntervalInit, pollingIntervalMultiplier, pollingTimeout);
    }

    protected ClientResponse upload(String pipelineName, String appName, Map<String, File> files) throws DeploymentException {
        final WebResource uploadRequest = baseResource.path("/direct/" + appName + "/" + pipelineName);

        final FormDataMultiPart form = new FormDataMultiPart();
        for (Map.Entry<String, File> file : files.entrySet()) {
            form.bodyPart(new FileDataBodyPart(file.getKey(), file.getValue()));
        }

        final ClientResponse uploadResponse = uploadRequest.type(MediaType.MULTIPART_FORM_DATA_TYPE).post(ClientResponse.class, form);
        if (HttpURLConnection.HTTP_ACCEPTED != uploadResponse.getStatus()) {
            final String defaultMessage = "Deploy not accepted";

            String customMessage = null;
            if (MediaType.APPLICATION_JSON_TYPE.isCompatible(uploadResponse.getType())) {
                final Map body = uploadResponse.getEntity(Map.class);
                if (body.containsKey("message")) {
                    customMessage = body.get("message").toString();
                }
            }

            throw new DeploymentException(customMessage != null ? customMessage : defaultMessage, uploadResponse.getEntity(String.class));
        }
        return uploadResponse;
    }

    protected Map<String, String> poll(ClientResponse uploadResponse, long pollingIntervalInit, double pollingIntervalMultiplier, long pollingTimeout) {
        final List<String> locationHeaders = uploadResponse.getHeaders().get("Location");
        if (locationHeaders == null || locationHeaders.get(0) == null) {
            throw new DeploymentException("Location header not found");
        }
        final String pollingUrl = locationHeaders.get(0);
        final WebResource pollingRequest = baseResource.path(pollingUrl);

        Map<String, String> response = stringify(uploadResponse.getEntity(Map.class));
        long pollingInterval = pollingIntervalInit;
        final long startTime = System.currentTimeMillis();
        while (STATUS_IN_PROCESS.equals(response.get(STATUS))) {
            response = stringify(pollingRequest.get(Map.class));

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

        return response;
    }

    private static Map<String, String> stringify(Map<?, ?> anyMap) {
        Map<String, String> stringMap = null;
        try {
            //noinspection unchecked
            stringMap = anyMap.getClass().getConstructor(int.class).newInstance(anyMap.size());
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        for (Map.Entry entry : anyMap.entrySet()) {
            stringMap.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return stringMap;
    }

    public static final class Builder {

        private String scheme = DEFAULT_SCHEME;
        private String host = DEFAULT_HOST;
        private int port = DEFAULT_PORT;
        private String apiKey;
        public String consumersUserAgent;

        public Builder setApiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder setScheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setConsumersUserAgent(String consumersUserAgent) {
            this.consumersUserAgent = consumersUserAgent;
            return this;
        }

        public DirectToHerokuClient build() {
            return new DirectToHerokuClient(this);
        }
    }
}
