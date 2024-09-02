package com.peew.notesr.tools;

import android.net.Uri;
import com.peew.notesr.App;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

public class FileExifDataResolverTest {

    private static final int MAX_FILE_SIZE = 100000;

    private static final Map<String, String> TYPES = Map.of(
            "txt", "text/plain",
            "jpg", "image/jpeg",
            "mp4", "video/mp4",
            "mp3", "audio/mpeg");

    private static final Random random = new Random();

    private static File testFile;
    private static int testFileSize;
    private static FileExifDataResolver resolver;

    @BeforeClass
    public static void beforeAll() throws IOException {
        File cacheDir = App.getContext().getCacheDir();
        String extension = new ArrayList<>(TYPES.keySet()).get(random.nextInt(TYPES.size()));

        testFile = File.createTempFile("test", "." + extension, cacheDir);
        testFileSize = random.nextInt(MAX_FILE_SIZE);

        byte[] testFileData = new byte[testFileSize];
        random.nextBytes(testFileData);

        try (FileOutputStream outputStream = new FileOutputStream(testFile)) {
            outputStream.write(testFileData);
        }

        resolver = new FileExifDataResolver(Uri.fromFile(testFile));
    }

    @Test
    public void testGetMimeType() {
        String extension = new LinkedList<>(Arrays.asList(testFile.getName().split("\\."))).getLast();

        String expected = TYPES.get(extension);
        String actual = resolver.getMimeType();

        Assert.assertEquals("Mime type are different", expected, actual);
    }

    @Test
    public void testGetFileName() {
        String expected = testFile.getName();
        String actual = resolver.getFileName();

        Assert.assertEquals("Names are different", expected, actual);
    }

    @Test
    public void testGetFileSize() {
        int actual = (int) resolver.getFileSize();
        Assert.assertEquals("Size are different", testFileSize, actual);
    }
}
