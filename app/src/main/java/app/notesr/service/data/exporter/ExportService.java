package app.notesr.service.data.exporter;

import static java.util.UUID.randomUUID;

import android.content.Context;
import android.content.pm.PackageManager;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import app.notesr.App;
import app.notesr.R;
import app.notesr.crypto.BackupCrypt;
import app.notesr.service.ServiceBase;
import app.notesr.service.data.TempDataWiper;
import app.notesr.utils.FilesUtils;
import app.notesr.utils.VersionFetcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

import app.notesr.utils.ZipUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExportService extends ServiceBase {
    
    private static final String VERSION_FILE_NAME = "version";
    private static final String NOTES_JSON_FILE_NAME = "notes.json";
    private static final String FILES_INFO_JSON_FILE_NAME = "files_info.json";
    private static final String DATA_BLOCKS_DIR_NAME = "data_blocks";

    private final Context context;
    private final File outputFile;

    private NotesExporter notesExporter;
    private FilesInfoExporter filesInfoExporter;
    private FilesDataExporter filesDataExporter;

    private File tempDir;
    private File tempArchive;

    private ExportThread thread;

    @Getter
    private ExportResult result = ExportResult.NONE;

    @Getter
    private String status = "";

    public void start() {
        if (getNoteTable().getRowsCount() == 0) {
            throw new RuntimeException("No notes in table");
        }

        thread = new ExportThread(this);
        thread.start();
    }

    public void cancel() {
        if (thread == null || !thread.isAlive()) {
            throw new IllegalStateException("Export has not been started");
        }

        status = context.getString(R.string.canceling);
        thread.interrupt();
    }

    public void onThreadInterrupted() {
        if (isFileExists(tempDir) || isFileExists(tempArchive)) {
            wipeTempData();
        }

        if (outputFile.exists()) {
            delete(outputFile);
        }

        status = "";
        result = ExportResult.CANCELED;
    }

    public int calculateProgress() {
        if (notesExporter == null || filesInfoExporter == null) {
            return 0;
        }

        if (result == ExportResult.FINISHED_SUCCESSFULLY) {
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

    void doExport() throws IOException, InterruptedException {
        init();
        export();
        archive();
        encrypt();
        wipe();
        finish();
    }

    private void init() throws IOException, InterruptedException {
        thread.breakOnInterrupted();
        status = context.getString(R.string.initializing);

        tempDir = new File(context.getCacheDir(), randomUUID().toString());

        if (!tempDir.mkdir()) {
            throw new RuntimeException("Failed to create temporary directory to export");
        }

        filesDataExporter = createFilesDataExporter();
        notesExporter = createNotesExporter(createJsonGenerator(tempDir, NOTES_JSON_FILE_NAME));

        filesInfoExporter = createFilesInfoExporter(
                createJsonGenerator(tempDir, FILES_INFO_JSON_FILE_NAME)
        );

        try {
            String version = VersionFetcher.fetchVersionName(context, false);
            File targetFile = new File(tempDir, VERSION_FILE_NAME);

            FilesUtils.writeFileBytes(targetFile, version.getBytes());
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void export() throws IOException, InterruptedException {
        thread.breakOnInterrupted();
        status = context.getString(R.string.exporting_data);

        notesExporter.export();
        filesInfoExporter.export();
        filesDataExporter.export();
    }

    private void archive() throws IOException, InterruptedException {
        thread.breakOnInterrupted();
        status = context.getString(R.string.compressing);

        tempArchive = new File(context.getCacheDir(), tempDir.getName() + ".zip");
        ZipUtils.zipDirectory(tempDir.getAbsolutePath(), tempArchive.getAbsolutePath(), thread);
    }

    private void encrypt() throws InterruptedException, IOException {
        thread.breakOnInterrupted();
        status = context.getString(R.string.encrypting_data);

        if (result == ExportResult.NONE) {
            FileInputStream inputStream = new FileInputStream(tempArchive);
            FileOutputStream outputStream = new FileOutputStream(outputFile);

            BackupCrypt backupCrypt = new BackupCrypt(inputStream, outputStream);
            backupCrypt.encrypt();
        }
    }

    private void wipe() throws InterruptedException {
        thread.breakOnInterrupted();
        status = context.getString(R.string.wiping_temp_data);

        wipeTempData();
    }

    void finish() {
        status = "";
        result = ExportResult.FINISHED_SUCCESSFULLY;
    }

    private void wipeTempData() {
        if (result == ExportResult.NONE) {
            TempDataWiper.wipeTempData(tempArchive, tempDir);
        }
    }

    private JsonGenerator createJsonGenerator(File tempDir, String filename) throws IOException {
        File file = new File(tempDir, filename);
        JsonFactory jsonFactory = new JsonFactory();

        return jsonFactory.createGenerator(file, JsonEncoding.UTF8);
    }

    private NotesExporter createNotesExporter(JsonGenerator jsonGenerator) {
        return new NotesExporter(
                thread,
                jsonGenerator,
                getNoteTable(),
                getTimestampFormatter()
        );
    }

    private FilesInfoExporter createFilesInfoExporter(JsonGenerator jsonGenerator) {
        return new FilesInfoExporter(
                thread,
                jsonGenerator,
                getFileInfoTable(),
                getDataBlockTable(),
                getTimestampFormatter()
        );
    }

    private FilesDataExporter createFilesDataExporter() {
        File dir = new File(tempDir, DATA_BLOCKS_DIR_NAME);
        return new FilesDataExporter(thread, dir, getDataBlockTable());
    }

    private DateTimeFormatter getTimestampFormatter() {
        return App.getAppContainer().getTimestampFormatter();
    }

    private void delete(File file) {
        if (!file.delete()) {
            throw new RuntimeException("Cannot delete file " + file.getAbsolutePath());
        }
    }

    private boolean isFileExists(File file) {
        return file != null && file.exists();
    }
}
