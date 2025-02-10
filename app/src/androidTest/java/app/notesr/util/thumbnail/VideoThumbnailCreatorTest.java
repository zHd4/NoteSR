package app.notesr.util.thumbnail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import app.notesr.App;

public class VideoThumbnailCreatorTest extends ThumbnailCreatorTestBase {
    @Test
    public void testGetThumbnail() throws IOException {
        File videoFile = getFixture("test_video.mp4");
        File thumbnailFile = File.createTempFile("thumbnail", "");

        Context context = App.getContext();
        VideoThumbnailCreator thumbnailCreator = new VideoThumbnailCreator(context);

        byte[] thumbnailBytes = thumbnailCreator.getThumbnail(videoFile);
        assertNotNull("Thumbnail creator returned null", thumbnailBytes);

        Files.write(Path.of(thumbnailFile.getAbsolutePath()), thumbnailBytes);

        int expectedWidth = VideoThumbnailCreator.WIDTH;
        int actualWidth = getImageSize(thumbnailFile).getWidth();

        assertEquals("Unexpected thumbnail width", expectedWidth, actualWidth);
    }
}
