package app.notesr.utils.thumbnail;

import static org.junit.Assert.assertEquals;

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
                {"test_image.jpeg"},
                {"test_image.jpg"},
                {"test_image.png"},
                {"test_image.webp"}
        });
    }

    @Test
    public void testGetThumbnail() throws IOException {
        File imageFile = getFixture(fixtureFileName);
        File thumbnailFile = File.createTempFile("thumbnail", "");

        ImageThumbnailCreator thumbnailCreator = new ImageThumbnailCreator();
        byte[] thumbnailBytes = thumbnailCreator.getThumbnail(imageFile);

        Files.write(Path.of(thumbnailFile.getAbsolutePath()), thumbnailBytes);

        Size imageSize = getImageSize(imageFile);
        Size thumbnailSize = getImageSize(thumbnailFile);

        int scaleFactor = Math.min(
                imageSize.getWidth() / ImageThumbnailCreator.WIDTH,
                imageSize.getHeight() / ImageThumbnailCreator.HEIGHT
        );

        Size expectedSize = new Size(
                imageSize.getWidth() / scaleFactor,
                imageSize.getHeight() / scaleFactor
        );

        Size actualSize = new Size(thumbnailSize.getWidth(), thumbnailSize.getHeight());

        assertEquals("Unexpected thumbnail size", expectedSize, actualSize);
    }
}
