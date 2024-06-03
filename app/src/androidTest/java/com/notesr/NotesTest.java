package com.notesr;

import com.peew.notesr.App;
import com.peew.notesr.crypto.CryptoKey;
import com.peew.notesr.crypto.FilesCrypt;
import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.db.notes.tables.FilesTable;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.model.EncryptedNote;
import com.peew.notesr.model.File;
import com.peew.notesr.model.Note;

import org.junit.Assert;
import org.junit.Before;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

import io.bloco.faker.Faker;

public class NotesTest {
    private static final Faker faker = new Faker();
    private static CryptoKey cryptoKey;

    private final NotesTable notesTable = App.getAppContainer().getNotesDatabase().getNotesTable();
    private final FilesTable filesTable = App.getAppContainer().getNotesDatabase().getFilesTable();

    private Note testNote;
    private File testFile;

    @BeforeClass
    public static void beforeAll() throws NoSuchAlgorithmException {
        String password = faker.internet.password();
        cryptoKey = App.getAppContainer().getCryptoManager().generateNewKey(password);
    }

    @Before
    public void before() {
        testNote = new Note(faker.lorem.word(), faker.lorem.paragraph());
        testFile = new File(faker.lorem.word(),
                faker.lorem.paragraph().getBytes(StandardCharsets.UTF_8));
    }

    @After
    public void after() {
        notesTable.getAll().forEach(note -> {
            filesTable.getByNoteId(note.getId()).forEach(file -> filesTable.delete(file.getId()));
            notesTable.delete(note.getId());
        });
    }

    @Test
    public void testCreateNote() {
        createAndGetNote();
    }

    @Test
    public void testUpdateNote() {
        Note note = createAndGetNote();

        note.setName(faker.lorem.word());
        note.setText(faker.lorem.paragraph());

        notesTable.save(NotesCrypt.encrypt(note, cryptoKey));

        EncryptedNote encryptedNote = notesTable.get(note.getId());
        Assert.assertNotNull(encryptedNote);

        Note actual = NotesCrypt.decrypt(encryptedNote, cryptoKey);

        Assert.assertNotNull(actual);
        Assert.assertEquals(note.getName(), actual.getName());
        Assert.assertEquals(note.getText(), actual.getText());
    }

    @Test
    public void testDeleteNote() {
        Note note = createAndGetNote();
        notesTable.delete(note.getId());

        EncryptedNote actual = notesTable.get(note.getId());
        Assert.assertNull(actual);
    }

    @Test
    public void testNoteAssignment() {
        Note note = createAndGetNote();

        testFile.setNoteId(note.getId());
        filesTable.save(FilesCrypt.encrypt(testFile, cryptoKey));

        List<File> noteFiles = FilesCrypt.decrypt(filesTable.getByNoteId(note.getId()), cryptoKey);
        Optional<File> fileOptional = noteFiles.stream().findFirst();

        Assert.assertTrue(fileOptional.isPresent());

        File file = fileOptional.get();

        Assert.assertEquals(note.getId(), file.getNoteId());
        Assert.assertEquals(testFile.getName(), file.getName());
        Assert.assertArrayEquals(testFile.getData(), file.getData());
    }

    private Note createAndGetNote() {
        notesTable.save(NotesCrypt.encrypt(testNote, cryptoKey));
        List<Note> allNotes = NotesCrypt.decrypt(notesTable.getAll(), cryptoKey);

        Assert.assertFalse(allNotes.isEmpty());

        Optional<Note> noteOptional = allNotes.stream()
                .filter(note -> note.getName().equals(testNote.getName()))
                .filter(note -> note.getText().equals(testNote.getText()))
                .findFirst();

        Assert.assertTrue(noteOptional.isPresent());

        return noteOptional.get();
    }
}
