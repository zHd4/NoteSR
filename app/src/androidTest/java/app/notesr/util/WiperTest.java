package app.notesr.util;

import app.notesr.App;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class WiperTest {
    private static final int MAX_FILE_SIZE = 100000;
    private final Random random = new Random();

    @Test
    public void testWipeFile() throws IOException {
        File cacheDir = App.getContext().getCacheDir();
        File testFile = File.createTempFile("test", "file", cacheDir);

        byte[] testData = new byte[random.nextInt(MAX_FILE_SIZE)];
        random.nextBytes(testData);

        try (FileOutputStream outputStream = new FileOutputStream(testFile)) {
            outputStream.write(testData);
        }

        boolean result = Wiper.wipeFile(testFile);
        Assert.assertTrue("File hasn't been wiped", result);
    }
}