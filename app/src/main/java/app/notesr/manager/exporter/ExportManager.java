package app.notesr.manager.exporter;

import static java.util.UUID.randomUUID;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import app.notesr.App;
import app.notesr.R;
import app.notesr.crypto.BackupsCrypt;
import app.notesr.manager.BaseManager;
import app.notesr.utils.FilesUtils;
import app.notesr.utils.Wiper;
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
public class ExportManager extends BaseManager {

    public static final int NONE = 0;
    public static final int FINISHED_SUCCESSFULLY = 2;
    public static final int CANCELED = -1;

    private static final String TAG = ExportManager.class.getName();

    private final Context context;
    private final File outputFile;

    private NotesExporter notesExporter;
    private FilesInfoExporter filesInfoExporter;
    private FilesDataExporter filesDataExporter;

    private File tempDir;
    private File tempArchive;

    private ExportThread thread;

    @Getter
    private int result = NONE;

    @Getter
    private String status = "";

    public void start() {
        if (getNotesTable().getRowsCount() == 0) {
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
        result = CANCELED;
    }

    public int calculateProgress() {
        if (notesExporter == null || filesInfoExporter == null) {
            return 0;
        }

        if (result == FINISHED_SUCCESSFULLY) {
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

    ExportManager init() throws IOException, InterruptedException {
        thread.breakOnInterrupted();
        status = context.getString(R.string.initializing);

        tempDir = new File(context.getCacheDir(), randomUUID().toString());

        if (!tempDir.mkdir()) {
            throw new RuntimeException("Failed to create temporary directory to export");
        }

        filesDataExporter = createFilesDataExporter();
        notesExporter = createNotesExporter(createJsonGenerator(tempDir, "notes.json"));

        filesInfoExporter = createFilesInfoExporter(
                createJsonGenerator(tempDir, "files_info.json")
        );

        try {
            String version = VersionFetcher.fetchVersionName(context, false);
            File targetFile = new File(tempDir, "version");

            FilesUtils.writeFileBytes(targetFile, version.getBytes());
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    ExportManager export() throws IOException, InterruptedException {
        thread.breakOnInterrupted();
        status = context.getString(R.string.exporting_data);

        notesExporter.export();
        filesInfoExporter.export();
        filesDataExporter.export();

        return this;
    }

    ExportManager archive() throws IOException, InterruptedException {
        thread.breakOnInterrupted();
        status = context.getString(R.string.compressing);

        tempArchive = new File(context.getCacheDir(), tempDir.getName() + ".zip");
        ZipUtils.zipDirectory(tempDir.getAbsolutePath(), tempArchive.getAbsolutePath(), thread);

        return this;
    }

    ExportManager encrypt() throws InterruptedException, IOException {
        thread.breakOnInterrupted();
        status = context.getString(R.string.encrypting_data);

        if (result == NONE) {
            FileInputStream inputStream = new FileInputStream(tempArchive);
            FileOutputStream outputStream = new FileOutputStream(outputFile);

            BackupsCrypt backupsCrypt = new BackupsCrypt(inputStream, outputStream);
            backupsCrypt.encrypt();
        }

        return this;
    }

    ExportManager wipe() throws InterruptedException {
        thread.breakOnInterrupted();
        status = context.getString(R.string.wiping_temp_data);

        wipeTempData();

        return this;
    }

    void finish() {
        status = "";
        result = FINISHED_SUCCESSFULLY;
    }

    private void wipeTempData() {
        if (result == NONE) {
            try {
                if (isFileExists(tempArchive)) {
                    Wiper.wipeFile(tempArchive);
                }

                if (isFileExists(tempDir)) {
                    Wiper.wipeDir(tempDir);
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
                throw new RuntimeException(e);
            }
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
                getNotesTable(),
                getTimestampFormatter()
        );
    }

    private FilesInfoExporter createFilesInfoExporter(JsonGenerator jsonGenerator) {
        return new FilesInfoExporter(
                thread,
                jsonGenerator,
                getFilesInfoTable(),
                getDataBlocksTable(),
                getTimestampFormatter()
        );
    }

    private FilesDataExporter createFilesDataExporter() {
        File dir = new File(tempDir, "data_blocks");
        return new FilesDataExporter(thread, dir, getDataBlocksTable());
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
