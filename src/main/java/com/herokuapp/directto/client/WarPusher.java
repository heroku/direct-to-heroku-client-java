package com.herokuapp.directto.client;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * @author Ryan Brainard
 */
public class WarPusher {

    public static final String DEFAULT_WARPATH_SERVICE_URI = "http://directto.herokuapp.com/push";
    public static final Pattern SUCCESSFUL_PUSH_PATTERN = Pattern.compile("Created new Slug based tomcat runner release v\\d+ for app [a-z0-9\\-]+.*");

    private final String apiKey;
    private String warpathServiceUri;

    public WarPusher(String apiKey) {
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

        final HttpResponse response = httpclient.execute(httpPost);

        final ByteOutputStream responseOut = new ByteOutputStream();
        response.getEntity().writeTo(responseOut);
        responseOut.close();
        final String responseBody = new String(responseOut.getBytes());

        // TODO: consider combining with status code checking once that is accurately returned
        if (!SUCCESSFUL_PUSH_PATTERN.matcher(responseBody).matches()) {
            throw new RuntimeException(responseBody);
        }

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new RuntimeException("UNKNOWN EXCEPTION:\n" +
                    response.getStatusLine() + "\n" +
                    responseBody);
        }
    }
}