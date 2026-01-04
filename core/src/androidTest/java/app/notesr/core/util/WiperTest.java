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
    private static final int MIN_FILE_SIZE = 1024;
    private static final int MAX_FILE_SIZE = 1024 * 10;
    private static final Random RANDOM = new Random();

    private static Context context;
    private static Wiper wiper;

    @BeforeClass
    public static void beforeAll() {
        context = ApplicationProvider.getApplicationContext();
        wiper = new Wiper();
    }

    @Test
    public void testWipeFile() throws IOException {
        File cacheDir = context.getCacheDir();
        File testFile = File.createTempFile("test", "file", cacheDir);

        byte[] testData = getRandomFileData();
        Files.write(testFile.toPath(), testData);

        wiper.wipeFile(testFile);

        assertFalse(testFile.getAbsolutePath() + " must be deleted", testFile.exists());
    }

    @Test
    public void testWipeDir() throws IOException {
        File cacheDir = context.getCacheDir();
        File testDir = new File(cacheDir, randomUUID().toString());

        boolean isDirCreated = testDir.mkdir();
        assertTrue("Cannot create directory " + testDir.getAbsolutePath(), isDirCreated);

        File testFile = File.createTempFile("test", "file", testDir);

        byte[] testData = getRandomFileData();
        Files.write(testFile.toPath(), testData);

        wiper.wipeDir(testDir);

        assertFalse(testDir.getAbsolutePath() + " must be deleted", testDir.exists());
        assertFalse(testFile.getAbsolutePath() + " must be deleted", testFile.exists());
    }

    private byte[] getRandomFileData() {
        int size = RANDOM.nextInt(MAX_FILE_SIZE - MIN_FILE_SIZE + 1) + MIN_FILE_SIZE;
        byte[] data = new byte[size];

        RANDOM.nextBytes(data);
        return data;
    }
}