package app.notesr.util;

import app.notesr.App;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class WiperTest {
    private static final int MIN_FILE_SIZE = 1024;
    private static final int MAX_FILE_SIZE = 1024 * 10;
    private final Random random = new Random();

    @Test
    public void testWipeFile() throws IOException {
        File cacheDir = App.getContext().getCacheDir();
        File testFile = File.createTempFile("test", "file", cacheDir);

        int testFileSize = random.nextInt(MAX_FILE_SIZE - MIN_FILE_SIZE + 1) + MIN_FILE_SIZE;
        byte[] testData = new byte[testFileSize];

        random.nextBytes(testData);

        try (FileOutputStream outputStream = new FileOutputStream(testFile)) {
            outputStream.write(testData);
        }

        boolean result = Wiper.wipeFile(testFile);
        Assert.assertTrue("File hasn't been wiped", result);
    }
}