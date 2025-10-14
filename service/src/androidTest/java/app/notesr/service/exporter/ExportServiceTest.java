package app.notesr.service.exporter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static java.util.UUID.randomUUID;

import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;

import android.content.Context;

import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;

import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.FilesUtils;
import app.notesr.core.util.FilesUtilsAdapter;
import app.notesr.data.AppDatabase;
import app.notesr.data.model.FileInfo;
import app.notesr.data.model.Note;
import app.notesr.service.file.FileService;
import app.notesr.service.note.NoteService;

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

        CryptoManager cryptoManager = CryptoManagerProvider.getInstance(context);
        CryptoSecrets cryptoSecrets = cryptoManager.generateSecrets("password");

        AesCryptor cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(cryptoSecrets));
        FilesUtilsAdapter filesUtils = new FilesUtils();

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        noteService = new NoteService(db);
        fileService = new FileService(context, db, cryptor, filesUtils);
        outputFile = new File(context.getCacheDir(), randomUUID() + "_test.backup");
        statusHolder = new ExportStatusHolder((progress, status) -> {
        });

        exportService = new ExportService(context, db, noteService, fileService, outputFile,
                statusHolder, cryptoSecrets);
    }

    @After
    public void tearDown() throws IOException {
        db.close();
        Files.deleteIfExists(outputFile.toPath());
    }

    @Test
    public void testSuccessfulExport() throws Exception {
        Note note = createTestNote();
        FileInfo fileInfo = createTestFileInfo(note.getId());

        noteService.importNote(note);
        fileService.importFileInfo(fileInfo);
        fileService.saveFileData(fileInfo.getId(), getTestFileDataInputStream());

        exportService.doExport();

        assertTrue(outputFile.exists());
        assertEquals(ExportStatus.DONE, statusHolder.getStatus());
        assertEquals(100, statusHolder.getProgress());
    }

    @Test
    public void testCancelledExport() {
        for (int i = 0; i < 100; i++) {
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

    private ByteArrayInputStream getTestFileDataInputStream() {
        return new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5});
    }
}
