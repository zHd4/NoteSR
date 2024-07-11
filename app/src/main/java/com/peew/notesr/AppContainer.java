package com.peew.notesr;

import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.db.notes.NotesDB;
import com.peew.notesr.manager.AssignmentsManager;
import com.peew.notesr.manager.KeyUpdateManager;
import com.peew.notesr.manager.NotesManager;

public class AppContainer {
    private final NotesDB notesDB = new NotesDB();

    private final CryptoManager cryptoManager = new CryptoManager();
    private final NotesManager notesManager = new NotesManager();
    private final AssignmentsManager assignmentsManager = new AssignmentsManager();
    private final KeyUpdateManager keyUpdateManager = new KeyUpdateManager();

    public NotesDB getNotesDB() {
        return notesDB;
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

    public KeyUpdateManager getKeyUpdateManager() {
        return keyUpdateManager;
    }
}
