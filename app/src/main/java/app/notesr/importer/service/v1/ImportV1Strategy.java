package app.notesr.importer.service.v1;

import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import app.notesr.db.AppDatabase;

import app.notesr.importer.service.ImportFailedException;
import app.notesr.file.service.FileService;
import app.notesr.importer.service.ImportStrategy;
import app.notesr.importer.service.NotesJsonImporter;
import app.notesr.note.service.NoteService;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ImportV1Strategy implements ImportStrategy {
    private static final String TAG = ImportV1Strategy.class.getName();

    private final AppDatabase db;
    private final NoteService noteService;
    private final FileService fileService;
    private final File tempDecryptedBackupFile;
    private final DateTimeFormatter timestampFormatter;

    @Override
    public void execute() {
        db.runInTransaction(() -> {
            try {
                JsonFactory jsonFactory = new JsonFactory();
                JsonParser jsonParser = jsonFactory.createParser(tempDecryptedBackupFile);

                try (jsonParser) {
                    NotesJsonImporter notesImporter = getNotesImporter(jsonParser);
                    notesImporter.importNotes();

                    FilesV1JsonImporter filesImporter =
                            getFilesImporter(jsonParser, notesImporter.getAdaptedIdMap());
                    filesImporter.importFiles();
                }
            } catch (Exception e) {
                Log.e(TAG, "Import failed with exception", e);
                throw new ImportFailedException(e);
            }
        });
    }

    private NotesJsonImporter getNotesImporter(JsonParser parser) {
        return new NotesJsonImporter(
                parser,
                noteService,
                timestampFormatter
        );
    }

    private FilesV1JsonImporter getFilesImporter(JsonParser parser,
                                                 Map<String, String> adaptedNotesIdMap) {
        return new FilesV1JsonImporter(
                parser,
                fileService,
                adaptedNotesIdMap,
                timestampFormatter
        );
    }
}
