package com.herokuapp.directto.client;

import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;

import static com.herokuapp.directto.client.DirectToHerokuClient.STATUS;
import static com.herokuapp.directto.client.DirectToHerokuClient.STATUS_SUCCESS;
import static org.junit.Assert.assertEquals;

/**
 * @author Ryan Brainard
 */
public class DeploymentLoadTest extends DirectToHerokuClientBaseTest {

    @Test
    public void testLargeUpload() throws Exception {
        File largeFile = File.createTempFile("large", ".war");
        try {
            RandomAccessFile largeness = new RandomAccessFile(largeFile, "rw");
            largeness.setLength(100 * 1024 * 1024);
            largeness.close();

            assertEquals(STATUS_SUCCESS, client.deploy(WAR_PIPELINE, appName, createWarBundle(largeFile.getPath())).get(STATUS));
        } finally {
            largeFile.delete();
        }
    }
}