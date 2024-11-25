package app.notesr.manager.importer.v2;

import static java.util.UUID.randomUUID;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.io.File;
import java.io.IOException;
import java.util.List;

import app.notesr.R;
import app.notesr.exception.ImportFailedException;
import app.notesr.manager.importer.BaseImportManager;
import app.notesr.manager.importer.ImportResult;
import app.notesr.manager.importer.NotesImporter;
import app.notesr.utils.Wiper;
import app.notesr.utils.ZipUtils;
import lombok.Getter;

public class ImportManagerV2 extends BaseImportManager {

    private static final String TAG = ImportManagerV2.class.getName();

    private static final String NOTES_JSON_FILE_NAME = "notes.json";
    private static final String FILES_INFO_JSON_FILE_NAME = "files_info.json";
    private static final String DATA_BLOCKS_DIR_NAME = "data_blocks";

    @Getter
    private ImportResult result = ImportResult.NONE;

    @Getter
    private String status = "";

    private File tempDir;

    public ImportManagerV2(Context context, File file) {
        super(context, file);
    }

    @Override
    public void start() {
        Thread thread = new Thread(() -> {
            try {
                tempDir = new File(context.getCacheDir(), randomUUID().toString());

                status = context.getString(R.string.importing);
                ZipUtils.unzip(file.getAbsolutePath(), tempDir.getAbsolutePath(), null);

                begin();

                clearTables();
                importData();

                end();

                status = context.getString(R.string.wiping_temp_data);
                wipeTempData();

                result = ImportResult.FINISHED_SUCCESSFULLY;
            } catch (IOException | ImportFailedException e) {
                if (isTransactionStarted()) {
                    rollback();
                }

                wipeTempData();

                status = context.getString(R.string.cannot_import_data);
                result = ImportResult.IMPORT_FAILED;
            }
        });

        thread.start();
    }

    private void importData() throws ImportFailedException, IOException {
        File notesJsonFile = new File(tempDir, NOTES_JSON_FILE_NAME);
        File fileInfoJsonFile = new File(tempDir, FILES_INFO_JSON_FILE_NAME);
        File dataBlocksDir = new File(tempDir, DATA_BLOCKS_DIR_NAME);

        JsonParser notesParser = createJsonParser(notesJsonFile);
        JsonParser filesInfoParser = createJsonParser(fileInfoJsonFile);

        NotesImporter notesImporter = createNotesImporter(notesParser);
        FilesImporter filesImporter = createFilesImporter(filesInfoParser, dataBlocksDir);

        notesImporter.importNotes();
        filesImporter.importFilesData();
    }

    private JsonParser createJsonParser(File file) throws IOException {
        JsonFactory factory = new JsonFactory();
        return factory.createParser(file);
    }

    private NotesImporter createNotesImporter(JsonParser parser) {
        return new NotesImporter(parser, getNotesTable(), getTimestampFormatter());
    }

    private FilesImporter createFilesImporter(JsonParser parser, File dataBlocksDir) {
        return new FilesImporter(
                parser,
                getFilesInfoTable(),
                getDataBlocksTable(),
                dataBlocksDir,
                getTimestampFormatter()
        );
    }

    private void wipeTempData() {
        try {
            if (!Wiper.wipeAny(List.of(file, tempDir))) {
                throw new IllegalStateException("Temp data has not been wiped");
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            throw new RuntimeException(e);
        }
    }
}
