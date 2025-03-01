package app.notesr;

import app.notesr.crypto.CryptoManager;
import app.notesr.db.notes.NotesDB;
import app.notesr.db.service.ServicesDB;
import app.notesr.service.FileService;
import app.notesr.service.NoteService;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class AppContainer {
    private final NotesDB notesDB = new NotesDB();
    private final ServicesDB servicesDB = new ServicesDB();

    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CryptoManager cryptoManager = new CryptoManager();
    private final NoteService noteService = new NoteService();
    private final FileService fileService = new FileService();
}
