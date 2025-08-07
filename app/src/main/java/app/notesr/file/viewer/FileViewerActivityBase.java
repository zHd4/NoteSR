package app.notesr.file.viewer;

import static app.notesr.util.ActivityUtils.showToastMessage;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import app.notesr.App;
import app.notesr.R;
import app.notesr.ActivityBase;
import app.notesr.db.DatabaseProvider;
import app.notesr.file.FilesListActivity;
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
import java.util.concurrent.Executors;

public class FileViewerActivityBase extends ActivityBase {
    protected FileInfo fileInfo;
    protected FileService fileService;
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

        fileService = new FileService(DatabaseProvider.getInstance(getApplicationContext()));
        fileInfo = (FileInfo) getIntent().getSerializableExtra("fileInfo");

        if (fileInfo == null) {
            throw new RuntimeException("File info not provided");
        }

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

    private void executeTaskWithProgressDialog(Runnable task, Runnable postUiAction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                R.style.AlertDialogTheme);
        builder.setView(R.layout.progress_dialog_deleting).setCancelable(false);

        AlertDialog progressDialog = builder.create();

        Executors.newSingleThreadExecutor().execute(() -> {
            runOnUiThread(progressDialog::show);
            task.run();
            runOnUiThread(progressDialog::dismiss);

            if (postUiAction != null) {
                runOnUiThread(postUiAction);
            }
        });
    }

    private void returnToListActivity() {
        Intent intent = new Intent(App.getContext(), FilesListActivity.class)
                .putExtra("noteId", fileInfo.getNoteId())
                .putExtra("modified", true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivity(intent);
    }

    protected void saveFileOnClick() {
        File destFile = Paths.get(saveDir.toPath().toString(), fileInfo.getName()).toFile();

        Runnable task = () -> saveFile(destFile);

        Runnable postUiAction = () -> {
            String messageFormat = getResources().getString(R.string.saved_to);
            String message = String.format(messageFormat, destFile.getAbsolutePath());
            showToastMessage(this, message, Toast.LENGTH_LONG);
        };

        DialogInterface.OnClickListener onConfirm =
                fileSavingConfirmedOnClick(task, postUiAction, destFile);

        AlertDialog.Builder saveConfirmationDialogBuilder = new AlertDialog.Builder(this,
                R.style.AlertDialogTheme);

        saveConfirmationDialogBuilder.setView(R.layout.dialog_are_you_sure)
                .setTitle(R.string.warning)
                .setPositiveButton(R.string.save, onConfirm);

        saveConfirmationDialogBuilder.create().show();
    }

    private DialogInterface.OnClickListener fileSavingConfirmedOnClick(Runnable task,
                                                                       Runnable postUiAction,
                                                                       File destFile) {
        Runnable overwriteConfirmationDialog = () -> {
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(this, R.style.AlertDialogTheme);

            builder.setView(R.layout.dialog_file_already_exists)
                    .setTitle(R.string.warning)
                    .setPositiveButton(R.string.overwrite,
                            (dialog, result) ->
                                    executeTaskWithProgressDialog(task, postUiAction));

            builder.create().show();
        };

        return (dialog, result) -> {
            if (!destFile.exists()) {
                executeTaskWithProgressDialog(task, postUiAction);
            } else {
                overwriteConfirmationDialog.run();
            }
        };
    }

    private void saveFile(File destFile) {
        fileService.read(fileInfo.getId(), chunk -> {
            try {
                FilesUtils.writeFileBytes(destFile, chunk, true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    protected void deleteFileOnClick() {
        DialogInterface.OnClickListener onConfirm = (dialog, result) ->
                executeTaskWithProgressDialog(() ->
                        fileService.delete(fileInfo.getId()), this::returnToListActivity);

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setView(R.layout.dialog_action_cannot_be_undo)
                .setTitle(R.string.warning)
                .setPositiveButton(R.string.delete, onConfirm);

        builder.create().show();
    }

    protected File dropToCache(FileInfo fileInfo) {
        try {
            String name = generateTempName(fileInfo.getId(), fileInfo.getName());
            String extension = FilesUtils.getFileExtension(fileInfo.getName());

            File tempDir = getCacheDir();
            File tempFile = File.createTempFile(name, "." + extension, tempDir);

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
}
