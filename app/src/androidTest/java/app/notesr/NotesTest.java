package app.notesr;

import app.notesr.dto.CryptoKey;
import app.notesr.crypto.FileCrypt;
import app.notesr.crypto.NoteCrypt;
import app.notesr.db.notes.table.DataBlockTable;
import app.notesr.db.notes.table.FileInfoTable;
import app.notesr.db.notes.table.NotesTable;
import app.notesr.model.DataBlock;
import app.notesr.model.EncryptedFileInfo;
import app.notesr.model.EncryptedNote;
import app.notesr.dto.FileInfo;
import app.notesr.dto.Note;
import io.bloco.faker.Faker;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class NotesTest {
    private static final Faker FAKER = new Faker();
    private static final Random RANDOM = new Random();

    private static CryptoKey cryptoKey;

    private final NotesTable notesTable = App.getAppContainer()
            .getNotesDB()
            .getTable(NotesTable.class);

    private final FileInfoTable fileInfoTable = App.getAppContainer()
            .getNotesDB()
            .getTable(FileInfoTable.class);

    private final DataBlockTable dataBlockTable = App.getAppContainer()
            .getNotesDB()
            .getTable(DataBlockTable.class);

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
        notesTable.getAll().forEach(note -> {
            fileInfoTable.getByNoteId(note.getId()).forEach(file -> {
                dataBlockTable.getBlocksIdsByFileId(file.getId()).forEach(dataBlockTable::delete);
                fileInfoTable.delete(file.getId());
            });

            notesTable.delete(note.getId());
        });
    }

    @Test
    public void testCreateNote() {
        saveTestNote();
        Assert.assertNotNull(testNote.getId());

        EncryptedNote encryptedActual = notesTable.get(testNote.getId());
        Assert.assertNotNull(encryptedActual);

        Note actual = NoteCrypt.decrypt(encryptedActual, cryptoKey);

        Assert.assertEquals(testNote.getName(), actual.getName());
        Assert.assertEquals(testNote.getText(), actual.getText());
        Assert.assertNotNull(actual.getUpdatedAt());
    }

    @Test
    public void testUpdateNote() {
        saveTestNote();
        Assert.assertNotNull(testNote.getId());

        String newName = FAKER.lorem.word();
        String newText = FAKER.lorem.paragraph();

        Note note = new Note(newName, newText);
        note.setId(testNote.getId());

        notesTable.save(NoteCrypt.encrypt(note, cryptoKey));

        EncryptedNote encryptedActual = notesTable.get(testNote.getId());
        Assert.assertNotNull(encryptedActual);

        Note actual = NoteCrypt.decrypt(encryptedActual, cryptoKey);

        Assert.assertEquals(actual.getName(), note.getName());
        Assert.assertEquals(actual.getText(), note.getText());
        Assert.assertNotNull(actual.getUpdatedAt());
    }

    @Test
    public void testAttachFile() {
        saveTestNote();
        Assert.assertNotNull(testNote.getId());

        byte[] testFileData = new byte[1024];
        RANDOM.nextBytes(testFileData);

        String fileName = FAKER.lorem.word();
        long fileSize = testFileData.length;

        FileInfo fileInfo = new FileInfo();

        fileInfo.setNoteId(testNote.getId());
        fileInfo.setSize(fileSize);
        fileInfo.setName(fileName);

        EncryptedFileInfo encryptedFileInfo = FileCrypt.encryptInfo(fileInfo, cryptoKey);
        fileInfoTable.save(encryptedFileInfo);

        DataBlock dataBlock = DataBlock.builder()
                .fileId(encryptedFileInfo.getId())
                .order(1L).data(testFileData)
                .build();

        dataBlockTable.save(dataBlock);

        Assert.assertNotNull(encryptedFileInfo.getId());
        Assert.assertNotNull(dataBlock.getId());

        Assert.assertEquals(encryptedFileInfo.getId(), dataBlock.getFileId());
        Assert.assertArrayEquals(dataBlockTable.get(dataBlock.getId()).getData(), dataBlock.getData());
    }

    @Test
    public void testDeleteNote() {
        saveTestNote();
        Assert.assertNotNull(testNote.getId());

        notesTable.delete(testNote.getId());
        EncryptedNote actual = notesTable.get(testNote.getId());

        Assert.assertNull(actual);
    }

    private void saveTestNote() {
        EncryptedNote encryptedNote = NoteCrypt.encrypt(testNote, cryptoKey);
        notesTable.save(encryptedNote);

        testNote.setId(encryptedNote.getId());
        testNote.setUpdatedAt(encryptedNote.getUpdatedAt());
    }
}
