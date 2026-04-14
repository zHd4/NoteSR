/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static java.util.UUID.randomUUID;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;

public class WiperTest {

    private static final int SMALL_FILE_SIZE = 10 * 1024;
    private static final int LARGE_FILE_SIZE = 128 * 1024; // Larger than Wiper.BUFFER_SIZE
    private static final Random RANDOM = new Random();

    private static Context context;
    private static WiperAdapter wiper;

    @BeforeClass
    public static void beforeAll() {
        context = ApplicationProvider.getApplicationContext();
        wiper = new Wiper();
    }

    @Test
    public void testWipeFile() throws IOException {
        File cacheDir = context.getCacheDir();
        File testFile = File.createTempFile("test", "file", cacheDir);

        byte[] testData = getRandomFileData(SMALL_FILE_SIZE);
        Files.write(testFile.toPath(), testData);

        wiper.wipeFile(testFile);

        assertFalse(testFile.getAbsolutePath() + " must be deleted", testFile.exists());
    }

    @Test
    public void testWipeEmptyFile() throws IOException {
        File cacheDir = context.getCacheDir();
        File testFile = File.createTempFile("test_empty", "file", cacheDir);

        wiper.wipeFile(testFile);

        assertFalse("Empty file must be deleted", testFile.exists());
    }

    @Test
    public void testWipeLargeFile() throws IOException {
        File cacheDir = context.getCacheDir();
        File testFile = File.createTempFile("test_large", "file", cacheDir);

        byte[] testData = getRandomFileData(LARGE_FILE_SIZE);
        Files.write(testFile.toPath(), testData);

        wiper.wipeFile(testFile);

        assertFalse("Large file must be deleted", testFile.exists());
    }

    @Test
    public void testWipeNestedDir() throws IOException {
        File cacheDir = context.getCacheDir();
        File parentDir = new File(cacheDir, "parent_" + randomUUID().toString());
        File childDir = new File(parentDir, "child_" + randomUUID().toString());

        assertTrue("Cannot create parent directory", parentDir.mkdir());
        assertTrue("Cannot create child directory", childDir.mkdir());

        File testFile = File.createTempFile("test", "file", childDir);
        Files.write(testFile.toPath(), getRandomFileData(SMALL_FILE_SIZE));

        wiper.wipeDir(parentDir);

        assertFalse("Parent directory must be deleted", parentDir.exists());
        assertFalse("Child directory must be deleted", childDir.exists());
        assertFalse("Nested file must be deleted", testFile.exists());
    }

    @Test
    public void testWipeEmptyDir() throws IOException {
        File cacheDir = context.getCacheDir();
        File emptyDir = new File(cacheDir, "empty_dir_" + randomUUID().toString());
        assertTrue("Cannot create empty directory", emptyDir.mkdir());

        wiper.wipeDir(emptyDir);

        assertFalse("Empty directory must be deleted", emptyDir.exists());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWipeFileNull() throws IOException {
        wiper.wipeFile(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWipeFileWithDir() throws IOException {
        wiper.wipeFile(context.getCacheDir());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWipeFileNotExists() throws IOException {
        wiper.wipeFile(new File(context.getCacheDir(), randomUUID().toString()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWipeDirNull() throws IOException {
        wiper.wipeDir(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWipeDirWithFile() throws IOException {
        File testFile = File.createTempFile("test", "file", context.getCacheDir());
        try {
            wiper.wipeDir(testFile);
        } finally {
            testFile.delete();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWipeDirNotExists() throws IOException {
        wiper.wipeDir(new File(context.getCacheDir(), randomUUID().toString()));
    }

    private byte[] getRandomFileData(int size) {
        byte[] data = new byte[size];
        RANDOM.nextBytes(data);
        return data;
    }
}