package app.notesr.exporter.service;

import static java.util.UUID.randomUUID;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import app.notesr.file.service.FileService;
import app.notesr.note.service.NoteService;
import app.notesr.security.crypto.BackupEncryptor;
import app.notesr.security.crypto.CryptoManager;
import app.notesr.security.crypto.CryptoManagerProvider;
import app.notesr.db.AppDatabase;
import app.notesr.security.dto.CryptoSecrets;
import app.notesr.exception.EncryptionFailedException;
import app.notesr.util.TempDataWiper;
import app.notesr.util.FilesUtils;
import app.notesr.util.VersionFetcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;

import app.notesr.util.ZipUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExportService {
    private static final String TAG = ExportService.class.getName();
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String VERSION_FILE_NAME = "version";
    private static final String NOTES_JSON_FILE_NAME = "notes.json";
    private static final String FILES_INFO_JSON_FILE_NAME = "files_info.json";
    private static final String DATA_BLOCKS_DIR_NAME = "data_blocks";

    private final Context context;
    private final AppDatabase db;
    private final NoteService noteService;
    private final FileService fileService;
    private final File outputFile;
    private final ExportStatusHolder statusHolder;

    private NotesExporter notesExporter;
    private FilesInfoExporter filesInfoExporter;
    private FilesDataExporter filesDataExporter;

    private File tempDir;
    private File tempArchive;
    private boolean isCancelled = false;

    public void doExport() {
        if (db.getNoteDao().getRowsCount() == 0) {
            throw new DataNotFoundException("No notes in table");
        }

        try {
            init();
            export();
            archive();
            encrypt();
            wipe();
            finish();
        } catch (EncryptionFailedException | IOException e) {
            Log.e(TAG, "Export failed", e);

            try {
                TempDataWiper.wipeTempData(tempArchive, tempDir);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            statusHolder.setStatus(ExportStatus.ERROR);

        } catch (ExportCancelledException e) {
            statusHolder.setStatus(ExportStatus.CANCELED);
        }
    }

    public void cancel() {
        isCancelled = true;
        statusHolder.setStatus(ExportStatus.CANCELLING);
    }

    private void checkCancelled() {
        if (isCancelled) {
            try {
                TempDataWiper.wipeTempData(tempArchive, tempDir);

                if (outputFile.exists()) {
                    Files.delete(outputFile.toPath());
                }

                throw new ExportCancelledException();
            } catch (IOException e) {
                throw new ExportCancelledException(e);
            }
        }
    }

    public int calculateProgress() {
        ExportStatus status = statusHolder.getStatus();

        if (status == null || status == ExportStatus.INITIALIZING) {
            return 0;
        } else if (status == ExportStatus.DONE) {
            return 100;
        }

        long total = notesExporter.getTotal()
                + filesInfoExporter.getTotal()
                + filesDataExporter.getTotal();

        long exported = notesExporter.getExported()
                + filesInfoExporter.getExported()
                + filesDataExporter.getExported();

        return Math.round((exported * 99.0f) / total);
    }

    private void init() throws IOException {
        checkCancelled();
        statusHolder.setStatus(ExportStatus.INITIALIZING);

        tempDir = new File(context.getCacheDir(), randomUUID().toString());

        if (!tempDir.mkdir()) {
            throw new ExportFailedException("Failed to create temporary directory to export");
        }

        filesDataExporter = createFilesDataExporter(new File(tempDir, DATA_BLOCKS_DIR_NAME));
        notesExporter = createNotesExporter(createJsonGenerator(tempDir, NOTES_JSON_FILE_NAME));

        filesInfoExporter = createFilesInfoExporter(
                createJsonGenerator(tempDir, FILES_INFO_JSON_FILE_NAME)
        );

        try {
            String version = VersionFetcher.fetchVersionName(context, false);
            File targetFile = new File(tempDir, VERSION_FILE_NAME);

            new FilesUtils().writeFileBytes(targetFile, version.getBytes());
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void export() throws IOException {
        checkCancelled();
        statusHolder.setStatus(ExportStatus.EXPORTING_DATA);

        notesExporter.export();
        filesInfoExporter.export();
        filesDataExporter.export();
    }

    private void archive() throws IOException {
        checkCancelled();
        statusHolder.setStatus(ExportStatus.COMPRESSING);

        tempArchive = new File(context.getCacheDir(), tempDir.getName() + ".zip");
        ZipUtils.zipDirectory(tempDir.getAbsolutePath(), tempArchive.getAbsolutePath());
    }

    private void encrypt() throws EncryptionFailedException, IOException {
        checkCancelled();
        statusHolder.setStatus(ExportStatus.ENCRYPTING_DATA);

        FileInputStream inputStream = new FileInputStream(tempArchive);
        FileOutputStream outputStream = new FileOutputStream(outputFile);

        CryptoManager cryptoManager = CryptoManagerProvider.getInstance();
        CryptoSecrets cryptoSecrets = cryptoManager.getSecrets();

        BackupEncryptor encryptor = new BackupEncryptor(cryptoSecrets, inputStream, outputStream);
        encryptor.encrypt();
    }

    private void wipe() throws IOException {
        checkCancelled();
        statusHolder.setStatus(ExportStatus.WIPING_TEMP_DATA);

        TempDataWiper.wipeTempData(tempArchive, tempDir);
    }

    void finish() {
        statusHolder.setStatus(ExportStatus.DONE);
    }

    private JsonGenerator createJsonGenerator(File tempDir, String filename) throws IOException {
        File file = new File(tempDir, filename);
        JsonFactory jsonFactory = new JsonFactory();

        return jsonFactory.createGenerator(file, JsonEncoding.UTF8);
    }

    private NotesExporter createNotesExporter(JsonGenerator jsonGenerator) {
        return new NotesExporter(
                jsonGenerator,
                noteService,
                this::checkCancelled,
                TIMESTAMP_FORMATTER
        );
    }

    private FilesInfoExporter createFilesInfoExporter(JsonGenerator jsonGenerator) {
        return new FilesInfoExporter(
                jsonGenerator,
                fileService,
                this::checkCancelled,
                TIMESTAMP_FORMATTER
        );
    }

    private FilesDataExporter createFilesDataExporter(File dataBlocksDir) {
        return new FilesDataExporter(dataBlocksDir, fileService, this::checkCancelled);
    }
}
