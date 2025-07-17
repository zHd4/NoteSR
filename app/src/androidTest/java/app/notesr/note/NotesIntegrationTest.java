package app.notesr.note;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import app.notesr.App;
import app.notesr.dto.CryptoKey;
import app.notesr.crypto.FileCryptor;
import app.notesr.crypto.NoteCryptor;
import app.notesr.db.notes.dao.DataBlockDao;
import app.notesr.db.notes.dao.FileInfoDao;
import app.notesr.db.notes.dao.NoteDao;
import app.notesr.model.DataBlock;
import app.notesr.model.EncryptedFileInfo;
import app.notesr.model.EncryptedNote;
import app.notesr.dto.FileInfo;
import app.notesr.dto.Note;
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

    private static CryptoKey cryptoKey;

    private final NoteDao noteTable = App.getAppContainer()
            .getNotesDB()
            .getTable(NoteDao.class);

    private final FileInfoDao fileInfoTable = App.getAppContainer()
            .getNotesDB()
            .getTable(FileInfoDao.class);

    private final DataBlockDao dataBlockTable = App.getAppContainer()
            .getNotesDB()
            .getTable(DataBlockDao.class);

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
        noteTable.getAll().forEach(note -> {
            fileInfoTable.getByNoteId(note.getId()).forEach(file -> {
                dataBlockTable.getBlocksIdsByFileId(file.getId()).forEach(dataBlockTable::delete);
                fileInfoTable.delete(file.getId());
            });

            noteTable.delete(note.getId());
        });
    }

    @Test
    public void testCreateNote() {
        saveTestNote();
        assertNotNull(testNote.getId());

        EncryptedNote encryptedActual = noteTable.get(testNote.getId());
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

        noteTable.save(NoteCryptor.encrypt(note, cryptoKey));

        EncryptedNote encryptedActual = noteTable.get(testNote.getId());
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
        fileInfoTable.save(encryptedFileInfo);

        DataBlock dataBlock = DataBlock.builder()
                .fileId(encryptedFileInfo.getId())
                .order(1L).data(testFileData)
                .build();

        dataBlockTable.save(dataBlock);

        assertNotNull(encryptedFileInfo.getId());
        assertNotNull(dataBlock.getId());

        assertEquals(encryptedFileInfo.getId(), dataBlock.getFileId());
        assertArrayEquals(dataBlockTable.get(dataBlock.getId()).getData(), dataBlock.getData());
    }

    @Test
    public void testDeleteNote() {
        saveTestNote();
        assertNotNull(testNote.getId());

        noteTable.delete(testNote.getId());
        EncryptedNote actual = noteTable.get(testNote.getId());

        assertNull(actual);
    }

    private void saveTestNote() {
        EncryptedNote encryptedNote = NoteCryptor.encrypt(testNote, cryptoKey);
        noteTable.save(encryptedNote);

        testNote.setId(encryptedNote.getId());
        testNote.setUpdatedAt(encryptedNote.getUpdatedAt());
    }
}
