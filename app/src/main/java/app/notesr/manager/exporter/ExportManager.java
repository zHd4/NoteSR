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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExportManager extends BaseManager {

    public static final int NONE = 0;
    public static final int FINISHED_SUCCESSFULLY = 2;
    public static final int CANCELED = -1;
    private static final String TAG = ExportManager.class.getName();

    @NonNull
    private final Context context;

    @NonNull
    private final File outputFile;

    private Thread thread;

    private NotesWriter notesWriter;
    private FilesInfoWriter filesInfoWriter;

    private File tempDir;
    private File tempArchive;

    @Getter
    private int result = NONE;

    @Getter
    private String status = "";

    public void start() {
        if (getNotesTable().getRowsCount() == 0) {
            throw new RuntimeException("No notes in table");
        }

        thread = new Thread(() -> {
            try {
                status = context.getString(R.string.exporting_data);
                tempDir = new File(context.getCacheDir(), randomUUID().toString());

                if (!tempDir.mkdir()) {
                    throw new RuntimeException("Failed to create temporary directory to export");
                }

                writeVersionFile(tempDir);

                notesWriter = createNotesWriter(createJsonGenerator(tempDir, "notes.json"));
                filesInfoWriter = createFilesInfoWriter(createJsonGenerator(tempDir, "files_info.json"));

                exportJson(notesWriter);
                exportJson(filesInfoWriter);
                exportFilesData();

                tempArchive = archiveTempDir();

                status = context.getString(R.string.encrypting_data);
                encryptTempArchive();

                status = context.getString(R.string.wiping_temp_data);
                wipeTempData();

                status = "";
                result = FINISHED_SUCCESSFULLY;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        thread.start();
    }

    public void cancel() {
        if (thread == null || !thread.isAlive()) {
            throw new IllegalStateException("Export has not been started");
        }

        status = context.getString(R.string.canceling);
        thread.interrupt();

        if (tempDir.exists() || tempArchive.exists()) {
            wipeTempData();
        }

        if (outputFile.exists()) {
            delete(outputFile);
        }

        status = "";
        result = CANCELED;
    }

    public int calculateProgress() {
        if (notesWriter == null || filesInfoWriter == null) {
            return 0;
        }

        if (result == FINISHED_SUCCESSFULLY) {
            return 100;
        }

        long total = notesWriter.getTotal() + filesInfoWriter.getTotal();
        long exported = notesWriter.getExported() + filesInfoWriter.getExported();

        return Math.round((exported * 99.0f) / total);
    }

    private void exportJson(JsonWriter writer) {
        try {
            if (result == NONE) {
                JsonGenerator generator = writer.getJsonGenerator();

                try (generator) {
                    writer.write();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            throw new RuntimeException(e);
        }
    }

    private void exportFilesData() throws IOException {
        File dir = new File(tempDir, "data_blocks");
        FilesDataExporter exporter = new FilesDataExporter(dir, getFilesInfoTable(), getDataBlocksTable());

        exporter.export();
    }

    private File archiveTempDir() throws IOException {
        File archiveFile = new File(context.getCacheDir(), tempDir.getName() + ".zip");
        ZipUtils.zipDirectory(tempDir.getAbsolutePath(), archiveFile.getAbsolutePath());

        return archiveFile;
    }

    private void encryptTempArchive() {
        if (result == NONE) {
            try {
                FileInputStream inputStream = new FileInputStream(tempArchive);
                FileOutputStream outputStream = new FileOutputStream(outputFile);

                BackupsCrypt backupsCrypt = new BackupsCrypt(inputStream, outputStream);
                backupsCrypt.encrypt();
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
                throw new RuntimeException(e);
            }
        }
    }

    private void wipeTempData() {
        if (result == NONE) {
            try {
                if (tempArchive.exists()) {
                    Wiper.wipeFile(tempArchive);
                }

                if (tempDir.exists()) {
                    Wiper.wipeDir(tempDir);
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
                throw new RuntimeException(e);
            }
        }
    }

    private void delete(File file) {
        if (!file.delete()) {
            throw new RuntimeException("Cannot delete file " + file.getAbsolutePath());
        }
    }

    private void writeVersionFile(File outputDir) throws IOException {
        try {
            String version = VersionFetcher.fetchVersionName(context, false);
            File targetFile = new File(outputDir, "version");

            FilesUtils.writeFileBytes(targetFile, version.getBytes());
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonGenerator createJsonGenerator(File tempDir, String filename) throws IOException {
        File file = new File(tempDir, filename);
        JsonFactory jsonFactory = new JsonFactory();

        return jsonFactory.createGenerator(file, JsonEncoding.UTF8);
    }

    private NotesWriter createNotesWriter(JsonGenerator jsonGenerator) {
        return new NotesWriter(
                jsonGenerator,
                getNotesTable(),
                getTimestampFormatter()
        );
    }

    private FilesInfoWriter createFilesInfoWriter(JsonGenerator jsonGenerator) {
        return new FilesInfoWriter(
                jsonGenerator,
                getFilesInfoTable(),
                getDataBlocksTable(),
                getTimestampFormatter()
        );
    }

    private DateTimeFormatter getTimestampFormatter() {
        return App.getAppContainer().getTimestampFormatter();
    }
}
