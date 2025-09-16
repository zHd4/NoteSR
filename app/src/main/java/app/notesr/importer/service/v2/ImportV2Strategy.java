package app.notesr.importer.service.v2;

import static java.util.UUID.randomUUID;
import static app.notesr.util.TempDataWiper.wipeTempData;

import android.content.Context;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import app.notesr.db.AppDatabase;
import app.notesr.exception.DecryptionFailedException;
import app.notesr.exception.ImportFailedException;
import app.notesr.file.service.FileService;
import app.notesr.importer.service.ImportStatusCallback;
import app.notesr.importer.service.ImportStrategy;
import app.notesr.importer.service.ImportStatus;
import app.notesr.importer.service.NotesJsonImporter;
import app.notesr.note.service.NoteService;
import app.notesr.util.ZipUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ImportV2Strategy implements ImportStrategy {

    private static final String NOTES_JSON_FILE_NAME = "notes.json";
    private static final String FILES_INFO_JSON_FILE_NAME = "files_info.json";
    private static final String DATA_BLOCKS_DIR_NAME = "data_blocks";

    private final Context context;
    private final AppDatabase db;
    private final NoteService noteService;
    private final FileService fileService;
    private final File file;
    private final ImportStatusCallback statusCallback;
    private final DateTimeFormatter timestampFormatter;

    private File tempDir;

    @Override
    public void execute() {
        db.runInTransaction(() -> {
            tempDir = new File(context.getCacheDir(), randomUUID().toString());

            statusCallback.updateStatus(ImportStatus.IMPORTING);
            ZipUtils.unzip(file.getAbsolutePath(), tempDir.getAbsolutePath());
            importData();

            statusCallback.updateStatus(ImportStatus.CLEANING_UP);
            wipeTempData(file, tempDir);
            return null;
        });
    }

    private void importData() throws ImportFailedException, IOException, DecryptionFailedException {
        File notesJsonFile = new File(tempDir, NOTES_JSON_FILE_NAME);
        File fileInfoJsonFile = new File(tempDir, FILES_INFO_JSON_FILE_NAME);
        File dataBlocksDir = new File(tempDir, DATA_BLOCKS_DIR_NAME);

        JsonParser notesParser = getJsonParser(notesJsonFile);
        JsonParser filesInfoParser = getJsonParser(fileInfoJsonFile);

        NotesJsonImporter notesImporter = getNotesImporter(notesParser);
        notesImporter.importNotes();

        getFilesImporter(filesInfoParser, dataBlocksDir, notesImporter.getAdaptedIdMap())
                .importFiles();
    }

    private JsonParser getJsonParser(File file) throws IOException {
        JsonFactory factory = new JsonFactory();
        return factory.createParser(file);
    }

    private NotesJsonImporter getNotesImporter(JsonParser parser) {
        return new NotesJsonImporter(parser, noteService, timestampFormatter);
    }

    private FilesV2JsonImporter getFilesImporter(
            JsonParser parser,
            File dataBlocksDir,
            Map<String, String> adaptedNotesIdMap) {
        return new FilesV2JsonImporter(
                parser,
                fileService,
                adaptedNotesIdMap,
                dataBlocksDir,
                timestampFormatter
        );
    }
}
