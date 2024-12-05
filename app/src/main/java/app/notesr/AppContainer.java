package app.notesr;

import app.notesr.crypto.CryptoManager;
import app.notesr.db.notes.NotesDB;
import app.notesr.db.services.ServicesDB;
import app.notesr.manager.AssignmentsManager;
import app.notesr.manager.KeyUpdateManager;
import app.notesr.manager.NotesManager;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class AppContainer {
    private final NotesDB notesDB = new NotesDB();
    private final ServicesDB servicesDB = new ServicesDB();

    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CryptoManager cryptoManager = new CryptoManager();
    private final NotesManager notesManager = new NotesManager();
    private final AssignmentsManager assignmentsManager = new AssignmentsManager();
    private final KeyUpdateManager keyUpdateManager = new KeyUpdateManager();
}
