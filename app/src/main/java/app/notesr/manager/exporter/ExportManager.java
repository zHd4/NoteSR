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
import app.notesr.utils.FileWiper;
import app.notesr.utils.VersionFetcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

import app.notesr.utils.ZipUtils;
import lombok.Getter;

public class ExportManager extends BaseManager {

    public static final int NONE = 0;
    public static final int FINISHED_SUCCESSFULLY = 2;
    public static final int CANCELED = -1;
    private static final String TAG = ExportManager.class.getName();

    private final Context context;
    private final File outputFile;

    private Thread thread;

    private NotesWriter notesWriter;
    private FilesInfoWriter filesInfoWriter;

    @Getter
    private int result = NONE;

    @Getter
    private String status = "";

    public ExportManager(Context context, File outputFile) {
        this.context = context;
        this.outputFile = outputFile;
    }

    public void start() {
        if (getNotesTable().getRowsCount() == 0) {
            throw new RuntimeException("No notes in table");
        }

        thread = new Thread(() -> {
            try {
                status = context.getString(R.string.exporting_data);
                File tempDir = new File(context.getCacheDir(), randomUUID().toString());

                if (!tempDir.mkdir()) {
                    throw new RuntimeException("Failed to create temporary directory to export");
                }

                writeVersion(tempDir);

                notesWriter = createNotesWriter(createJsonGenerator(tempDir, "notes.json"));
                filesInfoWriter = createFilesInfoWriter(createJsonGenerator(tempDir, "files_info.json"));

                exportJson(notesWriter);
                exportJson(filesInfoWriter);
                exportFilesData(new File(tempDir, "data_blocks"));

                status = context.getString(R.string.encrypting_data);
                status = context.getString(R.string.wiping_temp_data);
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

//        if (jsonTempFile.exists()) {
//            wipe(jsonTempFile);
//        }

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

    private void exportFilesData(File dir) throws IOException {
        FilesDataExporter exporter = new FilesDataExporter(dir, getFilesInfoTable(), getDataBlocksTable());
        exporter.export();
    }

    private File archiveDir(File dir) throws IOException {
        File archiveFile = new File(context.getCacheDir(), dir.getName() + ".zip");
        ZipUtils.zipDirectory(dir.getAbsolutePath(), archiveFile.getAbsolutePath());

        return archiveFile;
    }

    private void encrypt(File input, File output) {
        if (result == NONE) {
            try {
                FileInputStream inputStream = new FileInputStream(input);
                FileOutputStream outputStream = new FileOutputStream(output);

                BackupsCrypt backupsCrypt = new BackupsCrypt(inputStream, outputStream);
                backupsCrypt.encrypt();
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
                throw new RuntimeException(e);
            }
        }
    }

    private void wipe(File file) {
        if (result == NONE) {
            try {
                FileWiper fileWiper = new FileWiper(file);
                boolean success = fileWiper.wipeFile();

                if (!success) {
                    throw new RuntimeException("Filed to wipe file");
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

    private void writeVersion(File dir) throws IOException {
        try {
            String version = VersionFetcher.fetchVersionName(context, false);
            File targetFile = new File(dir, "version");

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
