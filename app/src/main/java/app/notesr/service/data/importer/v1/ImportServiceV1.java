package app.notesr.service.data.importer.v1;

import android.content.Context;
import android.util.Log;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import app.notesr.R;
import app.notesr.db.notes.NotesDb;
import app.notesr.db.notes.dao.DataBlockDao;
import app.notesr.db.notes.dao.FileInfoDao;
import app.notesr.db.notes.dao.NoteDao;
import app.notesr.exception.ImportFailedException;
import app.notesr.service.data.importer.ImportServiceBase;
import app.notesr.service.data.importer.ImportResult;
import app.notesr.service.data.importer.NotesImporter;
import app.notesr.util.Wiper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import lombok.Getter;

@Getter
public class ImportServiceV1 extends ImportServiceBase {
    private static final String TAG = ImportServiceV1.class.getName();

    private final NoteDao noteDao;
    private final FileInfoDao fileInfoDao;
    private final DataBlockDao dataBlockDao;

    private ImportResult result = ImportResult.NONE;
    private String status = "";

    public ImportServiceV1(Context context, NotesDb notesDb, File file) {
        super(context, notesDb, file);

        this.noteDao = notesDb.getDao(NoteDao.class);
        this.fileInfoDao = notesDb.getDao(FileInfoDao.class);
        this.dataBlockDao = notesDb.getDao(DataBlockDao.class);
    }

    @Override
    public void start() {
        Thread thread = new Thread(() -> {
            try {
                status = context.getString(R.string.importing);

                begin();
                importData(file);
                end();

                status = context.getString(R.string.wiping_temp_data);
                wipeFile(file);

                result = ImportResult.FINISHED_SUCCESSFULLY;
            } catch (ImportFailedException e) {
                if (isTransactionStarted()) {
                    rollback();
                }

                wipeFile(file);

                status = context.getString(R.string.cannot_import_data);
                result = ImportResult.IMPORT_FAILED;
            }
        });

        thread.start();
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
            Log.e(TAG, "Exception", e);
            throw new ImportFailedException(e);
        }
    }

    private NotesImporter getNotesImporter(JsonParser parser) {
        return new NotesImporter(
                parser,
                noteDao,
                getTimestampFormatter()
        );
    }

    private FilesImporter getFilesImporter(JsonParser parser, Map<String, String> adaptedNotesIdMap) {
        return new FilesImporter(
                parser,
                fileInfoDao,
                dataBlockDao,
                adaptedNotesIdMap,
                getTimestampFormatter()
        );
    }

    private void wipeFile(File file) {
        if (result == ImportResult.NONE) {
            try {
                boolean success = Wiper.wipeFile(file);

                if (!success) {
                    throw new RuntimeException("Filed to wipe file");
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
                throw new RuntimeException(e);
            }
        }
    }
}
