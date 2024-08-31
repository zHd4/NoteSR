package com.peew.notesr.tools;


import com.peew.notesr.App;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class FileWiperTest {

    private static final int MAX_FILE_SIZE = 100000;
    private final Random random = new Random();

    private File testFile;

    @Before
    public void before() throws IOException {
        byte[] testData = new byte[random.nextInt(MAX_FILE_SIZE)];
        random.nextBytes(testData);

        File cacheDir = App.getContext().getCacheDir();
        testFile = File.createTempFile("test", "file", cacheDir);

        try (FileOutputStream outputStream = new FileOutputStream(testFile)) {
            outputStream.write(testData);
        }
    }

    @Test
    public void testWipeFile() throws IOException {
        FileWiper wiper = new FileWiper(testFile);
        boolean result = wiper.wipeFile();

        Assert.assertTrue("File has not been wiped", result);
    }
}