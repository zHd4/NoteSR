package com.peew.notesr;

import com.peew.notesr.manager.AssignmentsManager;
import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.db.notes.NotesDatabase;
import com.peew.notesr.manager.NotesManager;

public class AppContainer {
    private final NotesDatabase notesDatabase = new NotesDatabase();
    private final CryptoManager cryptoManager = new CryptoManager();
    private final NotesManager notesManager = new NotesManager();
    private final AssignmentsManager assignmentsManager = new AssignmentsManager();

    public NotesDatabase getNotesDatabase() {
        return notesDatabase;
    }

    public CryptoManager getCryptoManager() {
        return cryptoManager;
    }

    public NotesManager getNotesManager() {
        return notesManager;
    }

    public AssignmentsManager getAssignmentsManager() {
        return assignmentsManager;
    }
}
