package app.notesr.core.util.thumbnail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import app.notesr.core.util.VideoThumbnailCreator;

public class VideoThumbnailCreatorTest extends ThumbnailCreatorTestBase {
    private static final int EXPECTED_WIDTH = 200;

    @Test
    public void testGetThumbnail() throws IOException {
        File videoFile = getFixture("test_video.mp4");
        File thumbnailFile = File.createTempFile("thumbnail", "");

        Context context = ApplicationProvider.getApplicationContext();
        app.notesr.core.util.VideoThumbnailCreator thumbnailCreator = new VideoThumbnailCreator(context);

        byte[] thumbnailBytes = thumbnailCreator.getThumbnail(Uri.fromFile(videoFile));
        assertNotNull("Thumbnail creator returned null", thumbnailBytes);

        Files.write(Path.of(thumbnailFile.getAbsolutePath()), thumbnailBytes);

        int actualWidth = getImageSize(thumbnailFile).getWidth();

        assertEquals("Unexpected thumbnail width", EXPECTED_WIDTH, actualWidth);
    }
}
