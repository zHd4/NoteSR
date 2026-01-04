/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.importer.v3;

import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.service.importer.ImportFailedException;
import app.notesr.data.model.FileBlobInfo;
import app.notesr.data.model.FileInfo;
import app.notesr.service.file.FileService;
import app.notesr.data.model.Note;
import app.notesr.service.note.NoteService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataImporterTest {

    @TempDir
    private Path tempDir;

    @Mock
    private BackupDecryptor decryptor;
    @Mock
    private NoteService noteService;
    @Mock
    private FileService fileService;

    private DataImporter importer;
    private Path zipPath;

    private AutoCloseable mocks;

    @BeforeEach
    void setup() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @Test
    void testImportNotesSuccess() throws Exception {
        String json = "{\"id\":1,\"name\":\"Test\",\"text\":\"Test\"}";
        zipPath = createZipWithEntry("notes/note1.json", json.getBytes());
        importer = new DataImporter(decryptor, noteService, fileService, zipPath);

        when(decryptor.decryptJsonObject(any())).thenReturn(json);

        importer.importData();

        verify(noteService).importNote(any(Note.class));
    }

    @Test
    void testImportFilesInfoSuccess() throws Exception {
        String json = "{\"id\":42,\"name\":\"file.txt\"}";
        zipPath = createZipWithEntry("finfo/file1.json", json.getBytes());
        importer = new DataImporter(decryptor, noteService, fileService, zipPath);

        when(decryptor.decryptJsonObject(any())).thenReturn(json);

        importer.importData();

        verify(fileService).importFileInfo(any(FileInfo.class));
    }

    @Test
    void testImportFilesDataSuccess() throws Exception {
        String blobInfoJson = "{\"id\":\"blob1\"}";
        Path zip = tempDir.resolve("test.zip");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("binfo/blob1.json"));
            zos.write(blobInfoJson.getBytes());
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("fblobs/blob1"));
            zos.write("BLOB".getBytes());
            zos.closeEntry();
        }

        importer = new DataImporter(decryptor, noteService, fileService, zip);
        when(decryptor.decryptJsonObject(any())).thenReturn(blobInfoJson);
        when(decryptor.decrypt(any())).thenReturn("BLOB".getBytes());

        importer.importData();

        verify(fileService).importFileBlobInfo(any(FileBlobInfo.class));
        verify(fileService).importFileBlobData(eq("blob1"), eq("BLOB".getBytes()));
    }

    @Test
    void testImportFilesDataMissingBlobThrows() throws Exception {
        String blobInfoJson = "{\"id\":\"blobX\"}";
        zipPath = createZipWithEntry("binfo/blobX.json", blobInfoJson.getBytes());
        importer = new DataImporter(decryptor, noteService, fileService, zipPath);

        when(decryptor.decryptJsonObject(any())).thenReturn(blobInfoJson);

        assertThrows(ImportFailedException.class, importer::importData);
    }

    @Test
    void testImportNotesDecryptFailsThrows() throws Exception {
        String json = "{\"id\":1}";
        zipPath = createZipWithEntry("notes/note1.json", json.getBytes());
        importer = new DataImporter(decryptor, noteService, fileService, zipPath);

        when(decryptor.decryptJsonObject(any())).thenThrow(new DecryptionFailedException());
        assertThrows(ImportFailedException.class, importer::importData);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    private Path createZipWithEntry(String name, byte[] content) throws IOException {
        Path zip = tempDir.resolve("test.zip");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            ZipEntry entry = new ZipEntry(name);
            zos.putNextEntry(entry);
            zos.write(content);
            zos.closeEntry();
        }

        return zip;
    }
}
