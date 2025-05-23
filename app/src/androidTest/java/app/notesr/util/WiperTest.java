package app.notesr.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static java.util.UUID.randomUUID;

import app.notesr.App;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;

public class WiperTest {
    private static final int MIN_FILE_SIZE = 1024;
    private static final int MAX_FILE_SIZE = 1024 * 10;
    private final Random random = new Random();

    @Test
    public void testWipeFile() throws IOException {
        File cacheDir = App.getContext().getCacheDir();
        File testFile = File.createTempFile("test", "file", cacheDir);

        byte[] testData = getRandomFileData();
        Files.write(testFile.toPath(), testData);

        boolean result = Wiper.wipeFile(testFile);

        assertTrue("Result of wipeFile must be 'true'", result);
        assertFalse(testFile.getAbsolutePath() + " must be deleted", testFile.exists());
    }

    @Test
    public void testWipeDir() throws IOException {
        File cacheDir = App.getContext().getCacheDir();
        File testDir = new File(cacheDir, randomUUID().toString());

        boolean isDirCreated = testDir.mkdir();
        assertTrue("Cannot create directory " + testDir.getAbsolutePath(), isDirCreated);

        File testFile = File.createTempFile("test", "file", testDir);

        byte[] testData = getRandomFileData();
        Files.write(testFile.toPath(), testData);

        boolean result = Wiper.wipeDir(testDir);

        assertTrue("Result of wipeDir must be 'true'", result);
        assertFalse(testDir.getAbsolutePath() + " must be deleted", testDir.exists());
        assertFalse(testFile.getAbsolutePath() + " must be deleted", testFile.exists());
    }

    private byte[] getRandomFileData() {
        int size = random.nextInt(MAX_FILE_SIZE - MIN_FILE_SIZE + 1) + MIN_FILE_SIZE;
        byte[] data = new byte[size];

        random.nextBytes(data);
        return data;
    }
}