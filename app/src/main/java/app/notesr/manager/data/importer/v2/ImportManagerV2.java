package app.notesr.manager.data.importer.v2;

import static java.util.UUID.randomUUID;
import static app.notesr.manager.data.TempDataWiper.wipeTempData;

import android.content.Context;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.io.File;
import java.io.IOException;

import app.notesr.R;
import app.notesr.exception.ImportFailedException;
import app.notesr.manager.data.importer.BaseImportManager;
import app.notesr.manager.data.importer.ImportResult;
import app.notesr.manager.data.importer.NotesImporter;
import app.notesr.utils.ZipUtils;
import lombok.Getter;

public class ImportManagerV2 extends BaseImportManager {

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
                wipeTempData(file, tempDir);

                result = ImportResult.FINISHED_SUCCESSFULLY;
            } catch (IOException | ImportFailedException e) {
                if (isTransactionStarted()) {
                    rollback();
                }

                wipeTempData(file, tempDir);

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
        filesImporter.importFiles();
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
}