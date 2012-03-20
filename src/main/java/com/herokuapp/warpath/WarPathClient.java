package com.herokuapp.warpath;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author Ryan Brainard
 */
public class WarPathClient {

    public static final String DEFAULT_WARPATH_SERVICE_URI = "http://warpath.herokuapp.com/push";

    private final String apiKey;
    private String warpathServiceUri;

    public WarPathClient(String apiKey) {
        this.apiKey = apiKey;
        this.warpathServiceUri = DEFAULT_WARPATH_SERVICE_URI;
    }

    public void push(String appName, File warFile) throws IOException {
        if (!warFile.exists()) {
            throw new FileNotFoundException();
        }

        final HttpClient httpclient = new DefaultHttpClient();
        final HttpPost httpPost = new HttpPost(warpathServiceUri);

        final MultipartEntity multipartEntity = new MultipartEntity();
        multipartEntity.addPart("apiKey", new StringBody(apiKey));
        multipartEntity.addPart("appName", new StringBody(appName));
        multipartEntity.addPart("war", new FileBody(warFile));
        httpPost.setEntity(multipartEntity);

        HttpResponse response = httpclient.execute(httpPost);
    }
}