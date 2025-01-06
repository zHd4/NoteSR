package app.notesr;

import java.nio.file.Path;

public class TestBase {
    protected static String generateFixturePath(String pathPart) {
        return Path.of("src/test/resources/fixtures", pathPart).toString();
    }
}
