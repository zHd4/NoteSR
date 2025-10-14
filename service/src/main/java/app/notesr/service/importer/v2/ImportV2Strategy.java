package app.notesr.service.importer.v2;

import static java.util.UUID.randomUUID;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import app.notesr.data.AppDatabase;
import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.core.security.exception.EncryptionFailedException;
import app.notesr.service.importer.ImportFailedException;
import app.notesr.service.file.FileService;
import app.notesr.service.importer.ImportStrategy;
import app.notesr.service.importer.NotesJsonImporter;
import app.notesr.service.note.NoteService;
import app.notesr.core.util.ZipUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ImportV2Strategy implements ImportStrategy {

    private static final String NOTES_JSON_FILE_NAME = "notes.json";
    private static final String FILES_INFO_JSON_FILE_NAME = "files_info.json";
    private static final String DATA_BLOCKS_DIR_NAME = "data_blocks";

    private final AppDatabase db;
    private final NoteService noteService;
    private final FileService fileService;
    private final File tempDecryptedBackupFile;
    private final File tempDir;
    private final DateTimeFormatter timestampFormatter;

    private File tempBackupDataDir;

    @Override
    public void execute() {
        db.runInTransaction(() -> {
            tempBackupDataDir = new File(tempDir, randomUUID().toString());

            ZipUtils.unzip(
                    tempDecryptedBackupFile.getAbsolutePath(),
                    tempBackupDataDir.getAbsolutePath()
            );

            importData();
            return null;
        });
    }

    private void importData()
            throws ImportFailedException,
            EncryptionFailedException,
            DecryptionFailedException,
            IOException {

        File notesJsonFile = new File(tempBackupDataDir, NOTES_JSON_FILE_NAME);
        File fileInfoJsonFile = new File(tempBackupDataDir, FILES_INFO_JSON_FILE_NAME);
        File dataBlocksDir = new File(tempBackupDataDir, DATA_BLOCKS_DIR_NAME);

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
