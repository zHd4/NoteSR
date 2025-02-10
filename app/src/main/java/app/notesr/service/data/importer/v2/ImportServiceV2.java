package app.notesr.service.data.importer.v2;

import static java.util.UUID.randomUUID;
import static app.notesr.service.data.TempDataWiper.wipeTempData;

import android.content.Context;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import app.notesr.R;
import app.notesr.exception.ImportFailedException;
import app.notesr.service.data.importer.ImportServiceBase;
import app.notesr.service.data.importer.ImportResult;
import app.notesr.service.data.importer.NotesImporter;
import app.notesr.utils.ZipUtils;
import lombok.Getter;

public class ImportServiceV2 extends ImportServiceBase {

    private static final String NOTES_JSON_FILE_NAME = "notes.json";
    private static final String FILES_INFO_JSON_FILE_NAME = "files_info.json";
    private static final String DATA_BLOCKS_DIR_NAME = "data_blocks";

    @Getter
    private ImportResult result = ImportResult.NONE;

    @Getter
    private String status = "";

    private File tempDir;

    public ImportServiceV2(Context context, File file) {
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
        return new NotesImporter(parser, getNotesTable(), getTimestampFormatter());
    }

    private FilesImporter getFilesImporter(
            JsonParser parser,
            File dataBlocksDir,
            Map<String, String> adaptedNotesIdMap) {
        return new FilesImporter(
                parser,
                getFilesInfoTable(),
                getDataBlockTable(),
                adaptedNotesIdMap,
                dataBlocksDir,
                getTimestampFormatter()
        );
    }
}
