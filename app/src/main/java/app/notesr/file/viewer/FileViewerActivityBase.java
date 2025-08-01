package app.notesr.file.viewer;

import static app.notesr.util.ActivityUtils.showToastMessage;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import app.notesr.App;
import app.notesr.R;
import app.notesr.ActivityBase;
import app.notesr.file.FileListActivity;
import app.notesr.service.file.FileService;
import app.notesr.model.FileInfo;
import app.notesr.util.FilesUtils;
import app.notesr.util.HashHelper;
import lombok.Getter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileViewerActivityBase extends ActivityBase {
    protected FileInfo fileInfo;
    protected java.io.File saveDir;

    @Getter
    protected static boolean running;

    @Override
    protected void onStart() {
        super.onStart();
        running = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        running = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadFileInfo();

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(fileInfo.getName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_open_file, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.saveImageButton) {
            saveFileOnClick();
        } else if (id == R.id.deleteImageButton) {
            deleteFileOnClick();
        }

        return super.onOptionsItemSelected(item);
    }

    private void executeTask(Runnable task, int dialogLayoutResId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setView(dialogLayoutResId)
                .setCancelable(false);

        AlertDialog progressDialog = builder.create();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            runOnUiThread(progressDialog::show);
            task.run();
            runOnUiThread(progressDialog::dismiss);
        });
    }

    private void returnToListActivity() {
        Intent intent = new Intent(App.getContext(), FileListActivity.class)
                .putExtra("noteId", fileInfo.getNoteId())
                .putExtra("modified", true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivity(intent);
    }

    protected void loadFileInfo() {
        fileInfo = (FileInfo) getIntent().getSerializableExtra("fileInfo");

        if (fileInfo == null) {
            throw new RuntimeException("File info not provided");
        }
    }

    protected void saveFileOnClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setView(R.layout.dialog_are_you_sure)
                .setTitle(R.string.warning)
                .setPositiveButton(R.string.save, (dialog, result) -> saveFile())
                .setNegativeButton(R.string.no, (dialog, result) -> {});

        builder.create().show();
    }

    private void saveFile() {
        File destFile = Paths.get(saveDir.toPath().toString(), fileInfo.getName()).toFile();
        Runnable saveRunnable = getSaveRunnable(destFile);

        int dialogLayoutId = R.layout.progress_dialog_saving;

        if (destFile.exists()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                    .setView(R.layout.dialog_file_already_exists)
                    .setTitle(R.string.warning)
                    .setPositiveButton(R.string.overwrite, (dialog, result) -> executeTask(saveRunnable, dialogLayoutId))
                    .setNegativeButton(R.string.no, (dialog, result) ->
                            showToastMessage(this, getResources().getString(R.string.saving_canceled), Toast.LENGTH_SHORT));

            builder.create().show();
        } else {
            executeTask(saveRunnable, dialogLayoutId);
        }
    }

    private Runnable getSaveRunnable(File destFile) {
        return () -> {
            FileService fileService = App.getAppContainer().getFileService();

            try {
                fileService.read(fileInfo.getId(), chunk -> {
                    try {
                        FilesUtils.writeFileBytes(destFile, chunk, true);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                String messageFormat = getResources().getString(R.string.saved_to);

                Looper.prepare();
                showToastMessage(this, String.format(messageFormat, destFile.getAbsolutePath()), Toast.LENGTH_LONG);
            } catch (RuntimeException e) {
                Log.e("NoteSR", e.toString());

                Looper.prepare();
                showToastMessage(this, getResources().getString(R.string.cannot_save_file), Toast.LENGTH_SHORT);
            }
        };
    }

    protected void deleteFileOnClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setView(R.layout.dialog_action_cannot_be_undo)
                .setTitle(R.string.warning)
                .setNegativeButton(R.string.no, (dialog, result) -> {
                })
                .setPositiveButton(R.string.delete, (dialog, result) -> executeTask(() -> {
                    deleteFile();
                    returnToListActivity();
                }, R.layout.progress_dialog_deleting));

        builder.create().show();
    }

    protected File dropToCache(FileInfo fileInfo) {
        try {
            String name = generateTempName(fileInfo.getId(), fileInfo.getName());
            String extension = FilesUtils.getFileExtension(fileInfo.getName());

            File tempDir = getCacheDir();
            File tempFile = File.createTempFile(name, "." + extension, tempDir);

            FileService fileService = App.getAppContainer().getFileService();

            try (FileOutputStream stream = new FileOutputStream(tempFile)) {
                fileService.read(fileInfo.getId(), chunk -> {
                    try {
                        stream.write(chunk);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateTempName(String id, String name) {
        try {
            return HashHelper.toSha256String(id + "$" + name);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteFile() {
        App.getAppContainer()
                .getFileService()
                .delete(fileInfo.getId());
    }
}
