package app.notesr.importer.service.v1;

import android.util.Log;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import app.notesr.db.AppDatabase;

import app.notesr.exception.ImportFailedException;
import app.notesr.importer.service.ImportStatus;
import app.notesr.importer.service.ImportStrategy;
import app.notesr.importer.service.NotesImporter;
import app.notesr.util.Wiper;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ImportV1Strategy implements ImportStrategy {
    private static final String TAG = ImportV1Strategy.class.getName();

    private final AppDatabase db;
    private final File file;
    private final DateTimeFormatter timestampFormatter;

    @Getter
    private ImportStatus status;

    @Override
    public void execute() {
        try {
            db.runInTransaction(() -> {
                status = ImportStatus.IMPORTING;
                importData(file);

                status = ImportStatus.CLEANING_UP;
                wipeFile(file);

                status = ImportStatus.DONE;
            });
        } catch (ImportFailedException e) {
            status = ImportStatus.IMPORT_FAILED;
        }
    }

    private void importData(File file) throws ImportFailedException {
        try {
            JsonFactory jsonFactory = new JsonFactory();
            JsonParser jsonParser = jsonFactory.createParser(file);

            try (jsonParser) {
                NotesImporter notesImporter = getNotesImporter(jsonParser);
                notesImporter.importNotes();

                FilesImporter filesImporter =
                        getFilesImporter(jsonParser, notesImporter.getAdaptedIdMap());
                filesImporter.importFiles();
            }
        } catch (Exception e) {
            Log.e(TAG, "Import failed with exception", e);
            throw new ImportFailedException(e);
        }
    }

    private NotesImporter getNotesImporter(JsonParser parser) {
        return new NotesImporter(
                parser,
                db.getNoteDao(),
                timestampFormatter
        );
    }

    private FilesImporter getFilesImporter(JsonParser parser,
                                           Map<String, String> adaptedNotesIdMap) {
        return new FilesImporter(
                parser,
                db.getFileInfoDao(),
                db.getDataBlockDao(),
                adaptedNotesIdMap,
                timestampFormatter
        );
    }

    private void wipeFile(File file) {
        try {
            boolean success = Wiper.wipeFile(file);

            if (!success) {
                throw new RuntimeException("Filed to wipe file");
            }
        } catch (IOException e) {
            Log.e(TAG, "Filed to wipe file", e);
            throw new RuntimeException(e);
        }
    }
}
