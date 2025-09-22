package app.notesr;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import static app.notesr.util.KeyUtils.getSecretKeyFromSecrets;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import app.notesr.security.crypto.AesCryptor;
import app.notesr.security.crypto.AesGcmCryptor;
import app.notesr.security.crypto.CryptoManager;
import app.notesr.security.crypto.CryptoManagerProvider;
import app.notesr.db.AppDatabase;
import app.notesr.db.DatabaseProvider;
import app.notesr.security.dto.CryptoSecrets;
import app.notesr.file.model.FileBlobInfo;
import app.notesr.file.model.FileInfo;
import app.notesr.note.model.Note;
import app.notesr.file.service.FileService;
import app.notesr.note.service.NoteService;
import app.notesr.util.FilesUtils;
import app.notesr.util.FilesUtilsAdapter;
import io.bloco.faker.Faker;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class NotesIntegrationTest {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Faker FAKER = new Faker();

    private static Context context;
    private static AesCryptor cryptor;
    private static AppDatabase db;
    private static NoteService noteService;
    private static FileService fileService;

    private Note testNote;

    @BeforeClass
    public static void beforeAll() throws Exception {
        context = ApplicationProvider.getApplicationContext();

        FilesUtilsAdapter filesUtils = new FilesUtils();
        CryptoManager cryptoManager = CryptoManagerProvider.getInstance();
        CryptoSecrets cryptoSecrets = getTestSecrets();

        cryptoManager.setSecrets(context, cryptoSecrets);

        cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(cryptoSecrets));
        db = DatabaseProvider.getInstance(context);
        noteService = new NoteService(db);
        fileService = new FileService(context, db, cryptor, filesUtils);
    }

    @Before
    public void before() {
        testNote = new Note();

        testNote.setName(FAKER.lorem.word());
        testNote.setText(FAKER.lorem.paragraph());
    }

    @Test
    public void testCreateNote() {
        noteService.save(testNote);

        assertNotNull(testNote.getId());
        assertNotNull(testNote.getUpdatedAt());

        Note actual = db.getNoteDao().get(testNote.getId());

        assertNotNull(actual);
        assertEquals(testNote.getName(), actual.getName());
        assertEquals(testNote.getText(), actual.getText());
        assertNotNull(actual.getUpdatedAt());
    }

    @Test
    public void testUpdateNote() {
        noteService.save(testNote);

        assertNotNull(testNote.getId());
        assertNotNull(testNote.getUpdatedAt());

        LocalDateTime newUpdatedAt = LocalDateTime.now();

        testNote.setName(FAKER.lorem.word());
        testNote.setText(FAKER.lorem.paragraph());
        testNote.setUpdatedAt(newUpdatedAt);

        noteService.save(testNote);

        Note actual = db.getNoteDao().get(testNote.getId());

        assertNotNull(actual);

        assertEquals(testNote.getName(), actual.getName());
        assertEquals(testNote.getText(), actual.getText());

        LocalDateTime expectedUpdatedAt = testNote.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime actualUpdatedAt = actual.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS);

        assertEquals(expectedUpdatedAt, actualUpdatedAt);
    }

    @Test
    public void testAttachFile() throws Exception {
        noteService.save(testNote);

        assertNotNull(testNote.getId());
        assertNotNull(testNote.getUpdatedAt());

        byte[] fileData = new byte[1024];
        RANDOM.nextBytes(fileData);

        String fileName = FAKER.lorem.word();
        long fileSize = fileData.length;

        FileInfo fileInfo = new FileInfo();

        fileInfo.setNoteId(testNote.getId());
        fileInfo.setSize(fileSize);
        fileInfo.setName(fileName);

        fileService.saveFileInfo(fileInfo);

        assertNotNull(fileInfo.getId());

        assertEquals(testNote.getId(), fileInfo.getNoteId());
        assertEquals(fileSize, (long) fileInfo.getSize());
        assertEquals(fileName, fileInfo.getName());

        assertNull(fileInfo.getType());
        assertNull(fileInfo.getThumbnail());

        assertNotNull(fileInfo.getCreatedAt());
        assertNotNull(fileInfo.getUpdatedAt());

        ByteArrayInputStream fileDataStream = new ByteArrayInputStream(fileData);
        fileService.saveFileData(fileInfo.getId(), fileDataStream);

        String fileBlobId = db.getFileBlobInfoDao().getBlobIdsByFileId(fileInfo.getId()).get(0);
        FileBlobInfo fileBlobInfo = db.getFileBlobInfoDao().get(fileBlobId);

        assertNotNull(fileBlobInfo);

        assertEquals(fileInfo.getId(), fileBlobInfo.getFileId());
        assertEquals(0L, (long) fileBlobInfo.getOrder());

        File blobsDir = new File(context.getFilesDir(), FileService.BLOBS_DIR_NAME);
        File blobFile = new File(blobsDir, fileBlobId);

        byte[] fileBlobData = cryptor.decrypt(Files.readAllBytes(blobFile.toPath()));

        assertArrayEquals(fileData, fileBlobData);
    }

    @Test
    public void testDeleteNote() throws Exception {
        noteService.save(testNote);

        byte[] fileData = new byte[1024];
        RANDOM.nextBytes(fileData);

        String fileName = FAKER.lorem.word();
        long fileSize = fileData.length;

        FileInfo fileInfo = new FileInfo();

        fileInfo.setNoteId(testNote.getId());
        fileInfo.setSize(fileSize);
        fileInfo.setName(fileName);

        fileService.saveFileInfo(fileInfo);

        ByteArrayInputStream fileDataStream = new ByteArrayInputStream(fileData);
        fileService.saveFileData(fileInfo.getId(), fileDataStream);

        String fileBlobId = db.getFileBlobInfoDao().getBlobIdsByFileId(fileInfo.getId()).get(0);
        FileBlobInfo fileBlobInfo = db.getFileBlobInfoDao().get(fileBlobId);

        assertNotNull(testNote.getId());
        assertNotNull(testNote.getUpdatedAt());
        assertNotNull(fileInfo.getId());

        assertEquals(testNote.getId(), fileInfo.getNoteId());
        assertEquals(fileSize, (long) fileInfo.getSize());
        assertEquals(fileName, fileInfo.getName());

        assertNull(fileInfo.getType());
        assertNull(fileInfo.getThumbnail());

        assertNotNull(fileInfo.getCreatedAt());
        assertNotNull(fileInfo.getUpdatedAt());

        assertNotNull(fileBlobInfo);

        noteService.delete(testNote.getId(), fileService);

        assertNull(db.getNoteDao().get(testNote.getId()));
        assertNull(db.getFileInfoDao().get(fileInfo.getId()));
        assertNull(db.getFileBlobInfoDao().get(fileBlobId));
    }

    private static CryptoSecrets getTestSecrets() {
        byte[] key = new byte[CryptoManager.KEY_SIZE];
        RANDOM.nextBytes(key);

        String password = FAKER.internet.password();
        return new CryptoSecrets(key, password);
    }
}
