package com.peew.notesr;

import com.peew.notesr.crypto.CryptoKey;
import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.db.notes.tables.DataBlocksTable;
import com.peew.notesr.db.notes.tables.FilesTable;
import com.peew.notesr.db.notes.tables.NotesTable;
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
import java.time.LocalDateTime;

public class NotesTest {
    private static final Faker faker = new Faker();
    private static CryptoKey cryptoKey;

    private final NotesTable notesTable = App.getAppContainer()
            .getNotesDatabase()
            .getTable(NotesTable.class);

    private final FilesTable filesTable = App.getAppContainer()
            .getNotesDatabase()
            .getTable(FilesTable.class);

    private final DataBlocksTable dataBlocksTable = App.getAppContainer()
            .getNotesDatabase()
            .getTable(DataBlocksTable.class);

    private Note testNote;
    private FileInfo testFileInfo;
    private byte[] testFileData;

    @BeforeClass
    public static void beforeAll() throws NoSuchAlgorithmException {
        String password = faker.internet.password();
        cryptoKey = App.getAppContainer().getCryptoManager().generateNewKey(password);
    }

    @Before
    public void before() {
        LocalDateTime now = LocalDateTime.now();

        String noteName = faker.lorem.word();
        String noteText = faker.lorem.paragraph();

        testNote = new Note(noteName, noteText);
        testFileData = faker.lorem.paragraph().getBytes(StandardCharsets.UTF_8);

        String fileName = faker.lorem.word();
        long fileSize = testFileData.length;

        testFileInfo = new FileInfo(null, null, fileSize, fileName, null, now, now);
    }

    @After
    public void after() {
        notesTable.getAll().forEach(note -> {
            filesTable.getByNoteId(note.getId()).forEach(file -> {
                dataBlocksTable.getBlocksIdsByFileId(file.getId()).forEach(dataBlocksTable::delete);
                filesTable.delete(file.getId());
            });

            notesTable.delete(note.getId());
        });
    }

    @Test
    public void testCreateNote() {
        notesTable.save(NotesCrypt.encrypt(testNote, cryptoKey));
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
        notesTable.save(NotesCrypt.encrypt(testNote, cryptoKey));
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
    public void testDeleteNote() {
        notesTable.save(NotesCrypt.encrypt(testNote, cryptoKey));
        Assert.assertNotNull(testNote.getId());

        notesTable.delete(testNote.getId());
        EncryptedNote actual = notesTable.get(testNote.getId());

        Assert.assertNull(actual);
    }
}
