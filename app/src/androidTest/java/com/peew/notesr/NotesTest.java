package com.peew.notesr;

import com.peew.notesr.component.AssignmentsManager;
import com.peew.notesr.crypto.CryptoKey;
import com.peew.notesr.crypto.FilesCrypt;
import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.db.notes.tables.FilesTable;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.model.EncryptedFileInfo;
import com.peew.notesr.model.EncryptedNote;
import com.peew.notesr.model.File;
import com.peew.notesr.model.FileInfo;
import com.peew.notesr.model.Note;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
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
        LocalDateTime now = LocalDateTime.now();
        testNote = new Note(faker.lorem.word(), faker.lorem.paragraph(), now);

        byte[] fileData = faker.lorem.paragraph().getBytes(StandardCharsets.UTF_8);
        testFile = new File(
                faker.lorem.word(),
                null,
                (long) fileData.length,
                now,
                now,
                fileData
        );
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
    public void testNoteAssignment() throws IOException {
        AssignmentsManager assignmentsManager = App.getAppContainer().getAssignmentsManager();
        Note note = createAndGetNote();

        testFile.setNoteId(note.getId());

        FileInfo testFileInfo = new FileInfo(
                testFile.getId(),
                testFile.getNoteId(),
                testFile.getSize(),
                testFile.getName(),
                testFile.getType(),
                testFile.getCreatedAt(),
                testFile.getUpdatedAt()
        );

        EncryptedFileInfo encryptedTestFileInfo = FilesCrypt.encryptInfo(testFileInfo, cryptoKey);
        byte[] encryptedData = FilesCrypt.encryptData(testFile.getData(), cryptoKey);

        filesTable.save(encryptedTestFileInfo);

        Long fileId = encryptedTestFileInfo.getId();
        assignmentsManager.save(fileId, encryptedData);

        EncryptedFileInfo actualEncrypted = filesTable.get(fileId);

        Assert.assertNotNull(actualEncrypted);

        File actual = new File(FilesCrypt.decryptInfo(actualEncrypted, cryptoKey));

        byte[] actualData = FilesCrypt.decryptData(assignmentsManager.get(fileId), cryptoKey);
        actual.setData(actualData);

        Assert.assertEquals(fileId, actual.getId());
        Assert.assertEquals(note.getId(), actual.getNoteId());
        Assert.assertEquals(testFile.getName(), actual.getName());
        Assert.assertEquals(testFile.getType(), actual.getType());
        Assert.assertEquals(testFile.getSize(), actual.getSize());
        Assert.assertArrayEquals(testFile.getData(), actual.getData());
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
