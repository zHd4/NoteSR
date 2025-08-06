package app.notesr.service.data.importer.v2;

import static java.util.UUID.randomUUID;
import static app.notesr.service.data.TempDataWiper.wipeTempData;

import android.content.Context;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import app.notesr.db.AppDatabase;
import app.notesr.exception.ImportFailedException;
import app.notesr.service.data.importer.ImportStrategy;
import app.notesr.service.data.importer.ImportStatus;
import app.notesr.service.data.importer.NotesImporter;
import app.notesr.util.ZipUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ImportV2Strategy implements ImportStrategy {

    private static final String NOTES_JSON_FILE_NAME = "notes.json";
    private static final String FILES_INFO_JSON_FILE_NAME = "files_info.json";
    private static final String DATA_BLOCKS_DIR_NAME = "data_blocks";

    private final Context context;
    private final AppDatabase db;
    private final File file;
    private final DateTimeFormatter timestampFormatter;

    @Getter
    private ImportStatus status;

    private File tempDir;

    @Override
    public void execute() {
        try {
            db.runInTransaction(() -> {
                tempDir = new File(context.getCacheDir(), randomUUID().toString());

                status = ImportStatus.IMPORTING;
                ZipUtils.unzip(file.getAbsolutePath(), tempDir.getAbsolutePath());
                importData();

                status = ImportStatus.CLEANING_UP;
                wipeTempData(file, tempDir);
                return null;
            });
        } catch (Exception e) {
            if (tempDir != null && tempDir.exists()) {
                try {
                    wipeTempData(tempDir);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            status = ImportStatus.IMPORT_FAILED;
        }
    }

    private void importData() throws ImportFailedException, IOException {
        File notesJsonFile = new File(tempDir, NOTES_JSON_FILE_NAME);
        File fileInfoJsonFile = new File(tempDir, FILES_INFO_JSON_FILE_NAME);
        File dataBlocksDir = new File(tempDir, DATA_BLOCKS_DIR_NAME);

        JsonParser notesParser = getJsonParser(notesJsonFile);
        JsonParser filesInfoParser = getJsonParser(fileInfoJsonFile);

        NotesImporter notesImporter = getNotesImporter(notesParser);
        notesImporter.importNotes();

        getFilesImporter(filesInfoParser, dataBlocksDir, notesImporter.getAdaptedIdMap()).importFiles();
    }

    private JsonParser getJsonParser(File file) throws IOException {
        JsonFactory factory = new JsonFactory();
        return factory.createParser(file);
    }

    private NotesImporter getNotesImporter(JsonParser parser) {
        return new NotesImporter(parser, db.getNoteDao(), timestampFormatter);
    }

    private FilesImporter getFilesImporter(
            JsonParser parser,
            File dataBlocksDir,
            Map<String, String> adaptedNotesIdMap) {
        return new FilesImporter(
                parser,
                db.getFileInfoDao(),
                db.getDataBlockDao(),
                adaptedNotesIdMap,
                dataBlocksDir,
                timestampFormatter
        );
    }
}
