package com.peew.notesr.tools;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.test.platform.app.InstrumentationRegistry;
import com.peew.notesr.App;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

public class FileExifDataResolverTest {

    private static final Random RANDOM = new Random();

    private static File testFile;
    private static int testFileSize;
    private static FileExifDataResolver resolver;

    @BeforeClass
    public static void beforeAll() throws IOException {
        Context context = App.getContext();
        File cacheDir = context.getCacheDir();

        testFile = File.createTempFile("image", ".jpg", cacheDir);

        AssetManager assets = InstrumentationRegistry.getInstrumentation()
                .getContext()
                .getResources()
                .getAssets();

        try (InputStream inputStream = assets.open("test-image.jpg")) {
            try (FileOutputStream outputStream = new FileOutputStream(testFile)) {
                byte[] imageBytes = inputStream.readAllBytes();
                testFileSize = imageBytes.length;

                outputStream.write(imageBytes);
            }
        }

        Uri uri = getImageContentUri(context, testFile, testFileSize);
        resolver = new FileExifDataResolver(uri);
    }

    @Test
    public void testGetMimeType() {
        String extension = new LinkedList<>(Arrays.asList(testFile.getName().split("\\."))).getLast();

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
        String filePath = imageFile.getAbsolutePath();
        String fileName = imageFile.getName();

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Images.Media._ID },
                MediaStore.Images.Media.DATA + "=? ",
                new String[] { filePath }, null);

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            cursor.close();

            return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.SIZE, size);
                return context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }
}
