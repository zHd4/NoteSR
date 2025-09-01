package app.notesr.exporter.service;

import static org.junit.Assert.*;

import static java.util.UUID.randomUUID;

import android.content.Context;

import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;

import app.notesr.db.AppDatabase;
import app.notesr.file.model.DataBlock;
import app.notesr.file.model.FileInfo;
import app.notesr.file.service.FileService;
import app.notesr.note.model.Note;
import app.notesr.note.service.NoteService;
import app.notesr.security.crypto.CryptoManager;
import app.notesr.security.crypto.CryptoManagerProvider;
import app.notesr.security.dto.CryptoSecrets;

public class ExportServiceTest {
    private AppDatabase db;
    private NoteService noteService;
    private FileService fileService;
    private File outputFile;
    private ExportStatusHolder statusHolder;
    private ExportService exportService;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        noteService = new NoteService(db);
        fileService = new FileService(db);
        outputFile = new File(context.getCacheDir(), randomUUID() + "_test.backup");
        statusHolder = new ExportStatusHolder((progress, status) -> {
        });

        CryptoManager cryptoManager = CryptoManagerProvider.getInstance(context);
        CryptoSecrets cryptoSecrets = cryptoManager.generateSecrets("password");

        exportService = new ExportService(context, db, noteService, fileService, outputFile,
                statusHolder, cryptoSecrets);
    }

    @After
    public void tearDown() throws IOException {
        db.close();
        Files.deleteIfExists(outputFile.toPath());
    }

    @Test
    public void testSuccessfulExport() {
        Note note = createTestNote();
        FileInfo fileInfo = createTestFileInfo(note.getId());
        DataBlock dataBlock = createTestDataBlock(fileInfo.getId());

        noteService.save(note);
        fileService.importFileInfo(fileInfo);
        fileService.importDataBlock(dataBlock);

        exportService.doExport();

        assertTrue(outputFile.exists());
        assertEquals(ExportStatus.DONE, statusHolder.getStatus());
        assertEquals(100, statusHolder.getProgress());
    }

    @Test
    public void testCancelledExport() {
        for (int i = 0; i < 10; i++) {
            noteService.save(createTestNote());
        }

        Thread exportThread = new Thread(() -> exportService.doExport());
        exportThread.start();

        try {
            Thread.sleep(100);

            exportService.cancel();
            exportThread.join();
        } catch (InterruptedException e) {
            fail("Test interrupted");
        }

        assertFalse(outputFile.exists());
        assertEquals(ExportStatus.CANCELED, statusHolder.getStatus());
    }

    private Note createTestNote() {
        Note note = new Note();

        note.setId(randomUUID().toString());
        note.setName("Test Note");
        note.setText("Test Content");
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        return note;
    }

    private FileInfo createTestFileInfo(String noteId) {
        FileInfo fileInfo = new FileInfo();

        fileInfo.setId(randomUUID().toString());
        fileInfo.setNoteId(noteId);
        fileInfo.setSize(6L);
        fileInfo.setName("test.txt");
        fileInfo.setCreatedAt(LocalDateTime.now());
        fileInfo.setUpdatedAt(LocalDateTime.now());

        return fileInfo;
    }

    private DataBlock createTestDataBlock(String fileId) {
        DataBlock dataBlock = new DataBlock();

        dataBlock.setId(randomUUID().toString());
        dataBlock.setFileId(fileId);
        dataBlock.setOrder(0L);
        dataBlock.setData(new byte[]{1, 2, 3, 4, 5});

        return dataBlock;
    }
}
