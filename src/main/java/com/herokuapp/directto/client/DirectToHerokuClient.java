package com.herokuapp.directto.client;

import com.herokuapp.directto.client.models.Pipeline;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
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

/**
 * @author Ryan Brainard
 */
public class DirectToHerokuClient {

    private final WebResource baseResource;
    private Map<String, Pipeline> base;

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

    public Collection<String> getPipelineNames() {
        //noinspection unchecked
        return baseResource.path("/pipelines").get(Map.class).keySet();
    }

    public Pipeline getPipeline(String pipelineName) {
        return baseResource.path("/pipelines/" + pipelineName).get(Pipeline.class);
    }

    public Map<String, String> deploy(String pipelineName, String appName, Map<String, File> files) throws InterruptedException {
        final WebResource deployRequest = baseResource.path("/direct/" + appName + "/" + pipelineName);

        final FormDataMultiPart form = new FormDataMultiPart();
        for (Map.Entry<String, File> file : files.entrySet()) {
            form.bodyPart(new FileDataBodyPart(file.getKey(), file.getValue()));
        }

        final ClientResponse deployResponse = deployRequest.type(MediaType.MULTIPART_FORM_DATA_TYPE).post(ClientResponse.class, form);

        final WebResource pollingRequest = baseResource.path(deployResponse.getHeaders().get("Location").get(0));
        Map response = deployResponse.getEntity(Map.class);
        while (response.get("status").equals("inprocess")) {
            response = pollingRequest.get(Map.class);
            Thread.sleep(1000);
        }

        //noinspection unchecked
        return (Map<String, String>) response;
    }
}
