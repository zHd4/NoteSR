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
import java.util.Map;
import java.util.Random;

public class FileExifDataResolverTest {

    private static final int MAX_FILE_SIZE = 100000;

    private static final Map<String, String> TYPES = Map.of(
            "txt", "text",
            "jpg", "image",
            "mp4", "video",
            "mp3", "audio");

    private static final Random random = new Random();

    private static File testFile;
    private static FileExifDataResolver resolver;

    @BeforeClass
    public static void beforeAll() throws IOException {
        File cacheDir = App.getContext().getCacheDir();
        String extension = new ArrayList<>(TYPES.keySet()).get(random.nextInt(TYPES.size()));

        testFile = File.createTempFile("test", "." + extension, cacheDir);

        byte[] testFileData = new byte[random.nextInt(MAX_FILE_SIZE)];
        random.nextBytes(testFileData);

        try (FileOutputStream outputStream = new FileOutputStream(testFile)) {
            outputStream.write(testFileData);
        }

        resolver = new FileExifDataResolver(Uri.fromFile(testFile));
    }

    @Test
    public void testGetFileName() {
        String expected = testFile.getName();
        String actual = resolver.getFileName();

        Assert.assertEquals("Names are different", expected, actual);
    }
}
