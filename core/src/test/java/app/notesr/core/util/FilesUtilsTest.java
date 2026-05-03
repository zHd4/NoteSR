/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@ExtendWith(MockitoExtension.class)
class FilesUtilsTest {

    private FilesUtils filesUtils;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        filesUtils = new FilesUtils();
    }

    @Test
    void testWriteAndReadFileBytes() throws IOException {
        File file = tempDir.resolve("test.txt").toFile();
        byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);

        filesUtils.writeFileBytes(file, data);

        byte[] readData = filesUtils.readFileBytes(file);
        assertArrayEquals(data, readData, "Read data should match written data");
    }

    @Test
    void testWriteFileBytesAppend() throws IOException {
        File file = tempDir.resolve("test_append.txt").toFile();
        byte[] data1 = "Part 1".getBytes(StandardCharsets.UTF_8);
        byte[] data2 = "Part 2".getBytes(StandardCharsets.UTF_8);

        filesUtils.writeFileBytes(file, data1, false);
        filesUtils.writeFileBytes(file, data2, true);

        byte[] readData = filesUtils.readFileBytes(file);
        String result = new String(readData, StandardCharsets.UTF_8);
        assertEquals("Part 1Part 2", result,
                "File content should be the concatenation of both writes");
    }

    @Test
    void testMoveFile() throws IOException {
        File source = tempDir.resolve("source.txt").toFile();
        File target = tempDir.resolve("target.txt").toFile();
        byte[] data = "Move me".getBytes(StandardCharsets.UTF_8);

        filesUtils.writeFileBytes(source, data);
        filesUtils.moveFile(source, target);

        assertFalse(source.exists(), "Source file should no longer exist after move");
        assertTrue(target.exists(), "Target file should exist after move");
        assertArrayEquals(data, filesUtils.readFileBytes(target),
                "Target file content should match original source content");
    }

    @Test
    void testDeleteFile() throws IOException {
        File file = tempDir.resolve("to_delete.txt").toFile();
        filesUtils.writeFileBytes(file, new byte[]{1, 2, 3});
        assertTrue(file.exists(), "File should exist before deletion");

        filesUtils.deleteFile(file);
        assertFalse(file.exists(), "File should not exist after deletion");
    }

    @Test
    void testGetFileExtension() {
        assertEquals("txt", filesUtils.getFileExtension("test.txt"),
                "Should extract simple extension");
        assertEquals("gz", filesUtils.getFileExtension("archive.tar.gz"),
                "Should extract last extension");
        assertNull(filesUtils.getFileExtension("no_extension"),
                "Should return null if no dot is present");
        assertEquals("", filesUtils.getFileExtension("file."),
                "Should return empty string if dot is at the end");
        assertEquals("", filesUtils.getFileExtension("."),
                "Should return empty string if filename is just a dot");
    }

    @Test
    void testGetInternalFile() {
        Context context = mock(Context.class);
        File internalDir = new File("/data/user/0/app/files");
        when(context.getFilesDir()).thenReturn(internalDir);

        File result = filesUtils.getInternalFile(context, "my_file.txt");

        assertEquals(new File(internalDir, "my_file.txt"), result,
                "Should resolve relative path against internal files directory");
    }

    @Test
    void testGetDatabaseFile() {
        Context context = mock(Context.class);
        File dbFile = new File("/data/user/0/app/databases/my.db");
        when(context.getDatabasePath("my.db")).thenReturn(dbFile);

        File result = filesUtils.getDatabaseFile(context, "my.db");

        assertEquals(dbFile.getAbsoluteFile(), result.getAbsoluteFile(),
                "Should resolve database file via context");
    }
}
