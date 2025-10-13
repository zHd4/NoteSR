package app.notesr.core.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileExifDataResolverTest {

    private static File testFile;
    private static int testFileSize;
    private static FileExifDataResolver resolver;

    @BeforeClass
    public static void beforeAll() throws IOException {
        Context context = ApplicationProvider.getApplicationContext();
        File cacheDir = context.getCacheDir();

        testFile = File.createTempFile("image", ".jpg", cacheDir);

        AssetManager assets = InstrumentationRegistry.getInstrumentation()
                .getContext()
                .getResources()
                .getAssets();

        try (InputStream inputStream = assets.open("test_image.jpg")) {
            try (FileOutputStream outputStream = new FileOutputStream(testFile)) {
                byte[] imageBytes = inputStream.readAllBytes();
                testFileSize = imageBytes.length;

                outputStream.write(imageBytes);
            }
        }

        Uri uri = getImageContentUri(context, testFile, testFileSize);
        resolver = new FileExifDataResolver(context, new FilesUtils(), uri);
    }

    @Test
    public void testGetMimeType() {
        String expected = "image/jpeg";
        String actual = resolver.getMimeType();

        Assert.assertEquals("Mime type are different", expected, actual);
    }

    @Test
    public void testGetFileName() {
        String expected = testFile.getName();
        String actual = resolver.getFileName();

        Assert.assertEquals("Names are different", expected, actual);
    }

    @Test
    public void testGetFileSize() {
        int actual = (int) resolver.getFileSize();
        Assert.assertEquals("Size are different", testFileSize, actual);
    }

    public static Uri getImageContentUri(Context context, File imageFile, int size) {
        String fileName = imageFile.getName();

        ContentResolver contentResolver = context.getContentResolver();
        Uri imagesCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.SIZE, size);

        return contentResolver.insert(imagesCollection, values);
    }
}
