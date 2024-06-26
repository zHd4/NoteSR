package com.peew.notesr;

import com.peew.notesr.component.AssignmentsManager;
import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.db.notes.NotesDatabase;

public class AppContainer {
    private final NotesDatabase notesDatabase = new NotesDatabase();
    private final CryptoManager cryptoManager = new CryptoManager();
    private final AssignmentsManager assignmentsManager = new AssignmentsManager();

    public NotesDatabase getNotesDatabase() {
        return notesDatabase;
    }

    public CryptoManager getCryptoManager() {
        return cryptoManager;
    }

    public AssignmentsManager getAssignmentsManager() {
        return assignmentsManager;
    }
}
