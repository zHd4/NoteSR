package com.notesr;

import com.peew.notesr.App;
import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.db.notes.tables.FilesTable;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.model.File;
import com.peew.notesr.model.Note;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import io.bloco.faker.Faker;

public class NotesTest {
    private final NotesTable notesTable = App.getAppContainer().getNotesDatabase().getNotesTable();
    private final FilesTable filesTable = App.getAppContainer().getNotesDatabase().getFilesTable();
    private final Faker faker = new Faker();

    private Note testNote;
    private File testFile;

    @Before
    public void before() {
        testNote = new Note(faker.lorem.word(), faker.lorem.paragraph());
        testFile = new File(faker.lorem.word(),
                faker.lorem.paragraph().getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testCreateNote() {
        notesTable.save(NotesCrypt.encrypt(testNote));
        List<Note> allNotes = NotesCrypt.decrypt(notesTable.getAll());

        Assert.assertFalse(allNotes.isEmpty());

        Optional<Note> noteOptional = allNotes.stream()
                .filter(note -> note.getName().equals(testNote.getName()))
                .filter(note -> note.getText().equals(testNote.getText()))
                .findFirst();

        Assert.assertTrue(noteOptional.isPresent());
    }
}
