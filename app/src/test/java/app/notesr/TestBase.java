package app.notesr;

import java.nio.file.Path;

public class TestBase {
    protected static String getFixturePath(String pathPart) {
        return Path.of("src/test/resources/fixtures", pathPart).toString();
    }

    protected static String getTempPath(String pathPart) {
        return Path.of(System.getProperty("java.io.tmpdir"), pathPart).toString();
    }
}
