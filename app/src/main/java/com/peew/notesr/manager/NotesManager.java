package com.peew.notesr.manager;

import com.peew.notesr.App;
import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.model.EncryptedNote;
import com.peew.notesr.model.Note;

public class NotesManager {
    public void save(Note note) {
        EncryptedNote encryptedNote = NotesCrypt.encrypt(note);
        getNotesTable().save(encryptedNote);
    }

    public Note get(Long id) {
        EncryptedNote encryptedNote = getNotesTable().get(id);
        return NotesCrypt.decrypt(encryptedNote);
    }

    public void delete(Long id) {
        getNotesTable().delete(id);
    }

    private NotesTable getNotesTable() {
        return App.getAppContainer()
                .getNotesDatabase()
                .getTable(NotesTable.class);
    }
}
