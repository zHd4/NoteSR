/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@ExtendWith(MockitoExtension.class)
class TransactionalFilesUtilTest {

    @Mock
    private Context context;

    private FilesUtilsAdapter baseUtils;
    private File cacheDir;
    private File filesDir;
    private File dbDir;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        baseUtils = new FilesUtils();
        cacheDir = Files.createDirectory(tempDir.resolve("cache")).toFile();
        filesDir = Files.createDirectory(tempDir.resolve("files")).toFile();
        dbDir = Files.createDirectory(tempDir.resolve("databases")).toFile();

        when(context.getCacheDir()).thenReturn(cacheDir);
    }

    @Test
    void testWriteFileStagesChanges() throws IOException {
        String txId = "test_tx_1";
        try (TransactionalFilesUtil tx = new TransactionalFilesUtil(context, baseUtils, txId)) {
            File originalFile = new File(filesDir, "note.txt");
            byte[] data = "Hello Transaction".getBytes(StandardCharsets.UTF_8);

            tx.writeFileBytes(originalFile, data);
            assertFalse(originalFile.exists(),
                    "Original file should not exist yet");

            assertArrayEquals(data, tx.readFileBytes(originalFile),
                    "Should be able to read staged data");
            assertTrue(tx.isStaged(originalFile),
                    "File should be marked as staged");

            File txDir = new File(cacheDir, "tx_" + txId);
            assertTrue(txDir.exists(), "Transaction directory should be created in cache");
        }
    }

    @Test
    void testCommitAppliesChanges() throws IOException {
        File originalFile = new File(filesDir, "note.txt");
        byte[] data = "Hello Commit".getBytes(StandardCharsets.UTF_8);

        try (TransactionalFilesUtil tx = new TransactionalFilesUtil(context, baseUtils)) {
            tx.writeFileBytes(originalFile, data);
            tx.commit();
            assertTrue(tx.isCommitted(), "Transaction should be marked as committed");
        }

        assertTrue(originalFile.exists(),
                "File should exist at original location after commit");
        assertArrayEquals(data, Files.readAllBytes(originalFile.toPath()),
                "Committed data should match");

        File[] cacheFiles = cacheDir.listFiles();
        assertTrue(cacheFiles == null || cacheFiles.length == 0,
                "Cache should be empty after commit");
    }

    @Test
    void testRollbackDiscardsChanges() throws IOException {
        File originalFile = new File(filesDir, "note.txt");
        byte[] data = "Hello Rollback".getBytes(StandardCharsets.UTF_8);

        try (TransactionalFilesUtil tx = new TransactionalFilesUtil(context, baseUtils)) {
            tx.writeFileBytes(originalFile, data);
            tx.rollback();
        }

        assertFalse(originalFile.exists(), "File should not exist after rollback");

        File[] cacheFiles = cacheDir.listFiles();
        assertTrue(cacheFiles == null || cacheFiles.length == 0,
                "Cache should be empty after rollback");
    }

    @Test
    void testDeleteFileStaging() throws IOException {
        File originalFile = new File(filesDir, "to_delete.txt");
        Files.write(originalFile.toPath(), "content".getBytes(StandardCharsets.UTF_8));

        try (TransactionalFilesUtil tx = new TransactionalFilesUtil(context, baseUtils)) {
            tx.deleteFile(originalFile);

            assertTrue(originalFile.exists(), "File should still exist before commit");
            assertThrows(IOException.class, () -> tx.readFileBytes(originalFile),
                    "Should throw when reading a file deleted in transaction");

            tx.commit();
        }

        assertFalse(originalFile.exists(), "File should be deleted after commit");
    }

    @Test
    void testMoveFileStaging() throws IOException {
        File source = new File(filesDir, "source.txt");
        File target = new File(filesDir, "target.txt");

        byte[] data = "Move me".getBytes(StandardCharsets.UTF_8);
        Files.write(source.toPath(), data);

        try (TransactionalFilesUtil tx = new TransactionalFilesUtil(context, baseUtils)) {
            tx.moveFile(source, target);

            assertTrue(source.exists(), "Source should still exist before commit");
            assertFalse(target.exists(), "Target should not exist before commit");

            tx.commit();
        }

        assertFalse(source.exists(), "Source should be deleted after move commit");
        assertTrue(target.exists(), "Target should exist after move commit");
        assertArrayEquals(data, Files.readAllBytes(target.toPath()),
                "Target data should match original source");
    }

    @Test
    void testRecovery() throws IOException {
        String txId = "recovery_tx";
        File originalFile = new File(filesDir, "recovered.txt");
        byte[] data = "Staged but not committed".getBytes(StandardCharsets.UTF_8);

        // First instance stages data but "crashes" (closes without commit/rollback)
        try (TransactionalFilesUtil tx1 = new TransactionalFilesUtil(context, baseUtils, txId)) {
            tx1.writeFileBytes(originalFile, data);
        }

        // Second instance with same ID should recover and find staged file
        try (TransactionalFilesUtil tx2 = new TransactionalFilesUtil(context, baseUtils, txId)) {
            assertTrue(tx2.isStaged(originalFile),
                    "Recovered transaction should have staged file");
            assertArrayEquals(data, tx2.readFileBytes(originalFile),
                    "Recovered transaction should read staged data");

            tx2.commit();
        }

        assertTrue(originalFile.exists(),
                "File should exist after recovered commit");
        assertArrayEquals(data, Files.readAllBytes(originalFile.toPath()),
                "Recovered data should match");
    }

    @Test
    void testGetInternalFileReturnsStagedIfPresent() throws IOException {
        when(context.getFilesDir()).thenReturn(filesDir);

        File originalFile = new File(filesDir, "internal.txt");
        byte[] data = "Internal data".getBytes(StandardCharsets.UTF_8);

        try (TransactionalFilesUtil tx = new TransactionalFilesUtil(context, baseUtils)) {
            tx.writeFileBytes(originalFile, data);

            File resolved = tx.getInternalFile(context, "internal.txt");

            assertNotEquals(originalFile.getAbsolutePath(), resolved.getAbsolutePath(),
                    "Resolved path should not be the original path");
            assertTrue(resolved.getAbsolutePath().contains(cacheDir.getAbsolutePath()),
                    "Resolved path should be within the cache directory");
            assertArrayEquals(data, baseUtils.readFileBytes(resolved),
                    "Staged file content should match");
        }
    }

    @Test
    void testGetDatabaseFileReturnsStagedIfPresent() throws IOException {
        when(context.getDatabasePath(anyString())).thenAnswer(invocation -> {
            String dbName = invocation.getArgument(0);
            return new File(dbDir, dbName);
        });

        File originalFile = new File(dbDir, "test.db");
        byte[] data = "Database data".getBytes(StandardCharsets.UTF_8);

        try (TransactionalFilesUtil tx = new TransactionalFilesUtil(context, baseUtils)) {
            tx.writeFileBytes(originalFile, data);

            File resolved = tx.getDatabaseFile(context, "test.db");

            assertNotEquals(originalFile.getAbsolutePath(), resolved.getAbsolutePath(),
                    "Resolved path should not be the original path");
            assertTrue(resolved.getAbsolutePath().contains(cacheDir.getAbsolutePath()),
                    "Resolved path should be within the cache directory");
            assertArrayEquals(data, baseUtils.readFileBytes(resolved),
                    "Staged file content should match");
        }
    }
}
