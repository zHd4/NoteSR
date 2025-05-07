package app.notesr.util.thumbnail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class VideoThumbnailCreatorTest extends ThumbnailCreatorTestBase {
    private static final int EXPECTED_WIDTH = 200;

    @Test
    public void testGetThumbnail() throws IOException {
        File videoFile = getFixture("test_video.mp4");
        File thumbnailFile = File.createTempFile("thumbnail", "");

        VideoThumbnailCreator thumbnailCreator = new VideoThumbnailCreator();

        byte[] thumbnailBytes = thumbnailCreator.getThumbnail(videoFile);
        assertNotNull("Thumbnail creator returned null", thumbnailBytes);

        Files.write(Path.of(thumbnailFile.getAbsolutePath()), thumbnailBytes);

        int actualWidth = getImageSize(thumbnailFile).getWidth();

        assertEquals("Unexpected thumbnail width", EXPECTED_WIDTH, actualWidth);
    }
}
