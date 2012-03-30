package com.herokuapp.directto.client;

import com.herokuapp.directto.client.models.Pipeline;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

/**
 * @author Ryan Brainard
 */
public class DirectToHerokuClient {

    private final String apiKey;
    private final WebResource baseResource;

    public DirectToHerokuClient(String apiKey) {
        this.apiKey = apiKey;

        final ClientConfig config = new DefaultClientConfig();
        config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        final Client jerseyClient = Client.create(config);

        baseResource = jerseyClient.resource("http://direct-to.herokuapp.com");
    }

    Pipeline getPipeline(String pipelineName) {
        return baseResource.path("/pipelines/" + pipelineName).get(Pipeline.class);
    }

}
