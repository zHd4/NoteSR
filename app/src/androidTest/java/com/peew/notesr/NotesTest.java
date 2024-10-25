package com.peew.notesr;

import com.peew.notesr.crypto.CryptoKey;
import com.peew.notesr.crypto.FilesCrypt;
import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.db.notes.table.DataBlocksTable;
import com.peew.notesr.db.notes.table.FilesInfoTable;
import com.peew.notesr.db.notes.table.NotesTable;
import com.peew.notesr.model.EncryptedFileInfo;
import com.peew.notesr.model.EncryptedNote;
import com.peew.notesr.model.FileInfo;
import com.peew.notesr.model.Note;
import io.bloco.faker.Faker;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

public class NotesTest {
    private static final Faker faker = new Faker();
    private static CryptoKey cryptoKey;

    private final NotesTable notesTable = App.getAppContainer()
            .getNotesDB()
            .getTable(NotesTable.class);

    private final FilesInfoTable filesInfoTable = App.getAppContainer()
            .getNotesDB()
            .getTable(FilesInfoTable.class);

    private final DataBlocksTable dataBlocksTable = App.getAppContainer()
            .getNotesDB()
            .getTable(DataBlocksTable.class);

    private Note testNote;


    @BeforeClass
    public static void beforeAll() throws NoSuchAlgorithmException {
        String password = faker.internet.password();
        cryptoKey = App.getAppContainer().getCryptoManager().generateNewKey(password);
    }

    @Before
    public void before() {
        String noteName = faker.lorem.word();
        String noteText = faker.lorem.paragraph();

        testNote = new Note(noteName, noteText);
    }

    @After
    public void after() {
        notesTable.getAll().forEach(note -> {
            filesInfoTable.getByNoteId(note.getId()).forEach(file -> {
                dataBlocksTable.getBlocksIdsByFileId(file.getId()).forEach(dataBlocksTable::delete);
                filesInfoTable.delete(file.getId());
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

        Note actual = NotesCrypt.decrypt(encryptedActual, cryptoKey);

        Assert.assertEquals(testNote.getName(), actual.getName());
        Assert.assertEquals(testNote.getText(), actual.getText());
        Assert.assertNotNull(actual.getUpdatedAt());
    }

    @Test
    public void testUpdateNote() {
        saveTestNote();
        Assert.assertNotNull(testNote.getId());

        String newName = faker.lorem.word();
        String newText = faker.lorem.paragraph();

        Note note = new Note(newName, newText);
        note.setId(testNote.getId());

        notesTable.save(NotesCrypt.encrypt(note, cryptoKey));

        EncryptedNote encryptedActual = notesTable.get(testNote.getId());
        Assert.assertNotNull(encryptedActual);

        Note actual = NotesCrypt.decrypt(encryptedActual, cryptoKey);

        Assert.assertEquals(actual.getName(), note.getName());
        Assert.assertEquals(actual.getText(), note.getText());
        Assert.assertNotNull(actual.getUpdatedAt());
    }

    @Test
    public void testAddAssignment() {
        saveTestNote();
        Assert.assertNotNull(testNote.getId());

        byte[] testFileData = faker.lorem.paragraph().getBytes(StandardCharsets.UTF_8);

        String fileName = faker.lorem.word();
        long fileSize = testFileData.length;

        FileInfo fileInfo = new FileInfo();

        fileInfo.setNoteId(testNote.getId());
        fileInfo.setSize(fileSize);
        fileInfo.setName(fileName);

        EncryptedFileInfo encryptedFileInfo = FilesCrypt.encryptInfo(fileInfo, cryptoKey);

        filesInfoTable.save(encryptedFileInfo);
        Assert.assertNotNull(encryptedFileInfo.getId());
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
        EncryptedNote encryptedNote = NotesCrypt.encrypt(testNote, cryptoKey);
        notesTable.save(encryptedNote);

        testNote.setId(encryptedNote.getId());
        testNote.setUpdatedAt(encryptedNote.getUpdatedAt());
    }
}
