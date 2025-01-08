package app.notesr.utils.thumbnail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static java.util.UUID.randomUUID;

import android.graphics.BitmapFactory;
import android.util.Size;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class ImageThumbnailCreatorTest extends ThumbnailCreatorTestBase {
    private final String fixtureFileName;

    public ImageThumbnailCreatorTest(String fixtureFileName) {
        this.fixtureFileName = fixtureFileName;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Example_image.jpeg"},
                {"Example_image.jpg"},
                {"Example_image.png"},
                {"Example_image.webp"}
        });
    }

    @Test
    public void testGetThumbnail() throws IOException {
        File origImage = getFixture(fixtureFileName);
        File thumbnailFile = new File(getTempPath(randomUUID().toString()));

        ImageThumbnailCreator thumbnailCreator = new ImageThumbnailCreator();
        byte[] thumbnail = thumbnailCreator.getThumbnail(origImage);

        Files.write(Path.of(thumbnailFile.getAbsolutePath()), thumbnail);

        assertTrue("Thumbnail not found", thumbnailFile.exists());

        Size origImageSize = getImageSize(origImage);
        Size thumbnailSize = getImageSize(thumbnailFile);

        int scaleFactor = Math.min(
                origImageSize.getWidth() / ImageThumbnailCreator.WIDTH,
                origImageSize.getHeight() / ImageThumbnailCreator.HEIGHT
        );

        int expectedWidth = origImageSize.getWidth() / scaleFactor;
        int expectedHeight = origImageSize.getHeight() / scaleFactor;

        assertEquals(expectedWidth, thumbnailSize.getWidth());
        assertEquals(expectedHeight, thumbnailSize.getHeight());
    }

    public static Size getImageSize(File file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        int width = options.outWidth;
        int height = options.outHeight;

        return new Size(width, height);
    }

    private static String getTempPath(String pathPart) {
        return Path.of(System.getProperty("java.io.tmpdir"), pathPart).toString();
    }
}
