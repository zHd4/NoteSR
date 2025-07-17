package app.notesr;

import app.notesr.crypto.CryptoManager;
import app.notesr.db.notes.NotesDb;
import app.notesr.db.notes.dao.DataBlockDao;
import app.notesr.db.notes.dao.FileInfoDao;
import app.notesr.db.notes.dao.NoteDao;
import app.notesr.db.service.ServicesDb;
import app.notesr.service.file.FileService;
import app.notesr.service.note.NoteService;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class AppContainer {
    private final NotesDb notesDB = new NotesDb();
    private final ServicesDb servicesDB = new ServicesDb();

    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CryptoManager cryptoManager = new CryptoManager();

    private final NoteService noteService = new NoteService(
            notesDB.getDao(NoteDao.class),
            notesDB.getDao(FileInfoDao.class),
            notesDB.getDao(DataBlockDao.class)
    );

    private final FileService fileService = new FileService(
            notesDB.getDao(NoteDao.class),
            notesDB.getDao(FileInfoDao.class),
            notesDB.getDao(DataBlockDao.class)
    );
}
