package com.peew.notesr.tools;

import com.peew.notesr.App;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

public class FileExifDataResolverTest {

    private static final Map<String, String> TYPES = Map.of(
            "txt", "text",
            "jpg", "image",
            "mp4", "video",
            "mp3", "audio");

    private static final Random random = new Random();

    private static File testFile;

    @BeforeClass
    public static void beforeAll() throws IOException {
        File cacheDir = App.getContext().getCacheDir();
        String extension = new ArrayList<>(TYPES.keySet()).get(random.nextInt(TYPES.size()));

        testFile = File.createTempFile("test", "." + extension, cacheDir);
    }
}
