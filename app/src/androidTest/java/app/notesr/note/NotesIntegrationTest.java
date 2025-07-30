package app.notesr.note;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import app.notesr.App;
import app.notesr.dto.CryptoSecrets;
import app.notesr.crypto.FileCryptor;
import app.notesr.crypto.NoteCryptor;
import app.notesr.db.notes.dao.DataBlockDao;
import app.notesr.db.notes.dao.FileInfoDao;
import app.notesr.db.notes.dao.NoteDao;
import app.notesr.model.DataBlock;
import app.notesr.model.EncryptedFileInfo;
import app.notesr.model.EncryptedNote;
import app.notesr.model.FileInfo;
import app.notesr.model.Note;
import io.bloco.faker.Faker;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class NotesIntegrationTest {
    private static final Faker FAKER = new Faker();
    private static final Random RANDOM = new Random();

    private static CryptoSecrets cryptoKey;

    private final NoteDao noteDao = App.getAppContainer()
            .getNotesDB()
            .getDao(NoteDao.class);

    private final FileInfoDao fileInfoDao = App.getAppContainer()
            .getNotesDB()
            .getDao(FileInfoDao.class);

    private final DataBlockDao dataBlockDao = App.getAppContainer()
            .getNotesDB()
            .getDao(DataBlockDao.class);

    private Note testNote;


    @BeforeClass
    public static void beforeAll() throws NoSuchAlgorithmException {
        String password = FAKER.internet.password();
        cryptoKey = App.getAppContainer().getCryptoManager().generateNewKey(password);
    }

    @Before
    public void before() {
        String noteName = FAKER.lorem.word();
        String noteText = FAKER.lorem.paragraph();

        testNote = new Note(noteName, noteText);
    }

    @After
    public void after() {
        noteDao.getAll().forEach(note -> {
            fileInfoDao.getByNoteId(note.getId()).forEach(file -> {
                dataBlockDao.getBlocksIdsByFileId(file.getId()).forEach(dataBlockDao::delete);
                fileInfoDao.delete(file.getId());
            });

            noteDao.delete(note.getId());
        });
    }

    @Test
    public void testCreateNote() {
        saveTestNote();
        assertNotNull(testNote.getId());

        EncryptedNote encryptedActual = noteDao.get(testNote.getId());
        assertNotNull(encryptedActual);

        Note actual = NoteCryptor.decrypt(encryptedActual, cryptoKey);

        assertEquals(testNote.getName(), actual.getName());
        assertEquals(testNote.getText(), actual.getText());
        assertNotNull(actual.getUpdatedAt());
    }

    @Test
    public void testUpdateNote() {
        saveTestNote();
        assertNotNull(testNote.getId());

        String newName = FAKER.lorem.word();
        String newText = FAKER.lorem.paragraph();

        Note note = new Note(newName, newText);
        note.setId(testNote.getId());

        noteDao.save(NoteCryptor.encrypt(note, cryptoKey));

        EncryptedNote encryptedActual = noteDao.get(testNote.getId());
        assertNotNull(encryptedActual);

        Note actual = NoteCryptor.decrypt(encryptedActual, cryptoKey);

        assertEquals(actual.getName(), note.getName());
        assertEquals(actual.getText(), note.getText());
        assertNotNull(actual.getUpdatedAt());
    }

    @Test
    public void testAttachFile() {
        saveTestNote();
        assertNotNull(testNote.getId());

        byte[] testFileData = new byte[1024];
        RANDOM.nextBytes(testFileData);

        String fileName = FAKER.lorem.word();
        long fileSize = testFileData.length;

        FileInfo fileInfo = new FileInfo();

        fileInfo.setNoteId(testNote.getId());
        fileInfo.setSize(fileSize);
        fileInfo.setName(fileName);

        EncryptedFileInfo encryptedFileInfo = FileCryptor.encryptInfo(fileInfo, cryptoKey);
        fileInfoDao.save(encryptedFileInfo);

        DataBlock dataBlock = DataBlock.builder()
                .fileId(encryptedFileInfo.getId())
                .order(1L).data(testFileData)
                .build();

        dataBlockDao.save(dataBlock);

        assertNotNull(encryptedFileInfo.getId());
        assertNotNull(dataBlock.getId());

        assertEquals(encryptedFileInfo.getId(), dataBlock.getFileId());
        assertArrayEquals(dataBlockDao.get(dataBlock.getId()).getData(), dataBlock.getData());
    }

    @Test
    public void testDeleteNote() {
        saveTestNote();
        assertNotNull(testNote.getId());

        noteDao.delete(testNote.getId());
        EncryptedNote actual = noteDao.get(testNote.getId());

        assertNull(actual);
    }

    private void saveTestNote() {
        EncryptedNote encryptedNote = NoteCryptor.encrypt(testNote, cryptoKey);
        noteDao.save(encryptedNote);

        testNote.setId(encryptedNote.getId());
        testNote.setUpdatedAt(encryptedNote.getUpdatedAt());
    }
}
