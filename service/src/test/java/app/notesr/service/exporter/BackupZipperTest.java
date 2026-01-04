/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */
 
package app.notesr.service.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class BackupZipperTest {

    @TempDir
    private Path tempDir;
    
    private File zipFile;

    @BeforeEach
    void setUp() {
        zipFile = tempDir.resolve("backup.zip").toFile();
    }

    @Test
    void testCreatesDirectoryStructure() throws IOException {
        BackupZipper backupZipper = new BackupZipper(zipFile);
        backupZipper.close();

        try (ZipFile zip = new ZipFile(zipFile)) {
            assertNotNull(zip.getEntry("notes/"));
            assertNotNull(zip.getEntry("finfo/"));
            assertNotNull(zip.getEntry("binfo/"));
            assertNotNull(zip.getEntry("fblobs/"));
        }
    }

    @Test
    void testAddsVersionFile() throws IOException {
        String version = "1.0.0";

        try (BackupZipper backupZipper = new BackupZipper(zipFile)) {
            backupZipper.addVersionFile(version);
        }

        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry versionEntry = zip.getEntry("version");

            assertNotNull(versionEntry);
            assertEquals(version.length(), versionEntry.getSize());
        }
    }

    @Test
    void testAddsNote() throws IOException {
        byte[] noteContent = "test note".getBytes();
        String noteId = "note1";

        try (BackupZipper backupZipper = new BackupZipper(zipFile)) {
            backupZipper.addNote(noteId, noteContent);
        }

        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry noteEntry = zip.getEntry("notes/" + noteId);

            assertNotNull(noteEntry);
            assertEquals(noteContent.length, noteEntry.getSize());
        }
    }

    @Test
    void testAddsFileInfo() throws IOException {
        byte[] fileInfo = "file info".getBytes();
        String fileId = "file1";

        try (BackupZipper backupZipper = new BackupZipper(zipFile)) {
            backupZipper.addFileInfo(fileId, fileInfo);
        }

        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry fileInfoEntry = zip.getEntry("finfo/" + fileId);

            assertNotNull(fileInfoEntry);
            assertEquals(fileInfo.length, fileInfoEntry.getSize());
        }
    }

    @Test
    void testAddsBlob() throws IOException {
        String blobId = "blob1";

        byte[] blobInfo = "example blob info".getBytes();
        byte[] blobData = "example blob data 123".getBytes();

        try (BackupZipper backupZipper = new BackupZipper(zipFile)) {
            backupZipper.addBlob(blobId, blobInfo, blobData);
        }

        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry blobInfoEntry = zip.getEntry("binfo/" + blobId);
            ZipEntry blobDataEntry = zip.getEntry("fblobs/" + blobId);

            assertNotNull(blobInfoEntry);
            assertNotNull(blobDataEntry);

            assertEquals(blobInfo.length, blobInfoEntry.getSize());
            assertEquals(blobData.length, blobDataEntry.getSize());
        }
    }

    @Test
    void testThrowsIOExceptionOnInvalidFile() {
        File invalidFile = new File("/invalid/path/backup.zip");
        assertThrows(IOException.class, () -> new BackupZipper(invalidFile).close());
    }
}
