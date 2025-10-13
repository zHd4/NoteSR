package app.notesr.core.util;

import java.nio.file.Path;

public class TestUtils {
    public static String getFixturePath(String pathPart) {
        return Path.of("src/test/resources/fixtures", pathPart).toString();
    }

    public static String getTempPath(String pathPart) {
        return Path.of(System.getProperty("java.io.tmpdir"), pathPart).toString();
    }
}
