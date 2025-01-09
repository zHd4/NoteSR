package app.notesr;

import app.notesr.crypto.CryptoManager;
import app.notesr.db.notes.NotesDB;
import app.notesr.db.services.ServicesDB;
import app.notesr.service.AssignmentsService;
import app.notesr.service.KeyUpdateService;
import app.notesr.service.NotesService;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class AppContainer {
    private final NotesDB notesDB = new NotesDB();
    private final ServicesDB servicesDB = new ServicesDB();

    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CryptoManager cryptoManager = new CryptoManager();
    private final NotesService notesService = new NotesService();
    private final AssignmentsService assignmentsManager = new AssignmentsService();
    private final KeyUpdateService keyUpdateService = new KeyUpdateService();
}
