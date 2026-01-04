/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */
 
package app.notesr.core.util.thumbnail;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.net.Uri;
import android.util.Size;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import app.notesr.core.util.FilesUtils;

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

        Context context = ApplicationProvider.getApplicationContext();
        FilesUtils filesUtils = new FilesUtils();

        ImageThumbnailCreator thumbnailCreator = new ImageThumbnailCreator(context, filesUtils);

        byte[] thumbnailBytes = thumbnailCreator.getThumbnail(Uri.fromFile(imageFile));

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
