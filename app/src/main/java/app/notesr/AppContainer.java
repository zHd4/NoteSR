package app.notesr;

import app.notesr.crypto.CryptoManager;
import app.notesr.db.notes.NotesDB;
import app.notesr.db.services.ServicesDB;
import app.notesr.manager.AssignmentsManager;
import app.notesr.manager.KeyUpdateManager;
import app.notesr.manager.NotesManager;

import java.time.format.DateTimeFormatter;

public class AppContainer {
    private final NotesDB notesDB = new NotesDB();
    private final ServicesDB servicesDB = new ServicesDB();

    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CryptoManager cryptoManager = new CryptoManager();
    private final NotesManager notesManager = new NotesManager();
    private final AssignmentsManager assignmentsManager = new AssignmentsManager();
    private final KeyUpdateManager keyUpdateManager = new KeyUpdateManager();

    public NotesDB getNotesDB() {
        return notesDB;
    }

    public ServicesDB getServicesDB() {
        return servicesDB;
    }

    public DateTimeFormatter getTimestampFormatter() {
        return timestampFormatter;
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
