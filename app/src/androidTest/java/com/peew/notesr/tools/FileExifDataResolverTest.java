package com.peew.notesr.tools;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.peew.notesr.App;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

public class FileExifDataResolverTest {

    private static final int MAX_FILE_SIZE = 100000;
    private static final Random RANDOM = new Random();

    private static File testFile;
    private static int testFileSize;
    private static FileExifDataResolver resolver;

    @BeforeClass
    public static void beforeAll() throws IOException {
        Context context = App.getContext();
        File cacheDir = context.getCacheDir();

        testFile = File.createTempFile("image", ".jpg", cacheDir);
        testFileSize = RANDOM.nextInt(MAX_FILE_SIZE);

        byte[] testFileData = new byte[testFileSize];
        RANDOM.nextBytes(testFileData);

        try (FileOutputStream outputStream = new FileOutputStream(testFile)) {
            outputStream.write(testFileData);
        }

        Uri uri = getImageContentUri(context, testFile);
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

    public static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.INTERNAL_CONTENT_URI,
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
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }
}
