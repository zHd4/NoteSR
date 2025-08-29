package app.notesr.exporter.service;

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
            assertNotNull(zip.getEntry("note/"));
            assertNotNull(zip.getEntry("finfo/"));
            assertNotNull(zip.getEntry("dblock/"));
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
            ZipEntry noteEntry = zip.getEntry("note/" + noteId);

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
    void testAddsDataBlock() throws IOException {
        byte[] dataBlock = "data block".getBytes();
        String blockId = "block1";

        try (BackupZipper backupZipper = new BackupZipper(zipFile)) {
            backupZipper.addDataBlock(blockId, dataBlock);
        }

        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry dataBlockEntry = zip.getEntry("dblock/" + blockId);

            assertNotNull(dataBlockEntry);
            assertEquals(dataBlock.length, dataBlockEntry.getSize());
        }
    }

    @Test
    void testThrowsIOExceptionOnInvalidFile() {
        File invalidFile = new File("/invalid/path/backup.zip");
        assertThrows(IOException.class, () -> new BackupZipper(invalidFile).close());
    }
}
