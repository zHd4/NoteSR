package notesr;

import notesr.crypto.CryptoManager;
import notesr.db.notes.NotesDB;
import notesr.db.services.ServicesDB;
import notesr.manager.AssignmentsManager;
import notesr.manager.KeyUpdateManager;
import notesr.manager.NotesManager;

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
