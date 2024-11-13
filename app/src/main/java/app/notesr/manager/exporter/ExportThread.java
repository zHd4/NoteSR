package app.notesr.manager.exporter;

import static java.util.UUID.randomUUID;

import static app.notesr.manager.exporter.ExportManager.FINISHED_SUCCESSFULLY;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.File;
import java.io.IOException;

import app.notesr.R;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ExportThread extends Thread {
    private static final String TAG = ExportThread.class.getName();

    @NonNull
    private final ExportManager manager;

    private boolean running = false;

    @Override
    public void start() {
        running = true;
        super.start();
    }

    @Override
    public void interrupt() {
        running = false;
        super.interrupt();
    }

    @Override
    public boolean isInterrupted() {
        boolean interrupted = super.isInterrupted();
        return !running || interrupted;
    }

    @Override
    public void run() {
        try {
            init();
            export();
            archive();
            encrypt();
            wipe();
            finish();
        } catch (IOException e) {
            if (isInterrupted()) {
                Log.i(TAG, "Seems export has been canceled", e);
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private void init() throws IOException {
        if (!isInterrupted()) {
            Context context = manager.getContext();
            manager.setStatus(context.getString(R.string.exporting_data));

            File tempDir = new File(context.getCacheDir(), randomUUID().toString());
            manager.setTempDir(tempDir);

            if (!tempDir.mkdir()) {
                throw new RuntimeException("Failed to create temporary directory to export");
            }

            manager.writeVersionFile(tempDir);
        }
    }

    private void export() throws IOException {
        if (!isInterrupted()) {
            File tempDir = manager.getTempDir();

            JsonGenerator notesGenerator = manager.createJsonGenerator(tempDir, "notes.json");
            JsonGenerator filesInfoGenerator = manager.createJsonGenerator(tempDir, "files_info.json");

            NotesWriter notesWriter = manager.createNotesWriter(notesGenerator);
            FilesInfoWriter filesInfoWriter = manager.createFilesInfoWriter(filesInfoGenerator);

            manager.setNotesWriter(notesWriter);
            manager.setFilesInfoWriter(filesInfoWriter);

            manager.exportJson(notesWriter);
            manager.exportJson(filesInfoWriter);

            manager.exportFilesData();
        }
    }

    private void archive() throws IOException {
        if (!isInterrupted()) {
            File archive = manager.archiveTempDir();
            manager.setTempArchive(archive);
        }
    }

    private void encrypt() {
        if (!isInterrupted()) {
            Context context = manager.getContext();

            manager.setStatus(context.getString(R.string.encrypting_data));
            manager.encryptTempArchive();
        }
    }

    private void wipe() {
        if (!isInterrupted()) {
            Context context = manager.getContext();

            manager.setStatus(context.getString(R.string.wiping_temp_data));
            manager.wipeTempData();
        }
    }

    private void finish() {
        manager.setStatus("");
        manager.setResult(FINISHED_SUCCESSFULLY);
    }
}
