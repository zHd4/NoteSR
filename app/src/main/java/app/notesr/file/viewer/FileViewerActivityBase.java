package app.notesr.file.viewer;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static app.notesr.util.ActivityUtils.showToastMessage;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import app.notesr.DialogFactory;
import app.notesr.R;
import app.notesr.ActivityBase;
import app.notesr.db.DatabaseProvider;
import app.notesr.file.FileIOHelper;
import app.notesr.file.FilesListActivity;
import app.notesr.service.file.FileService;
import app.notesr.model.FileInfo;

import java.io.File;

public class FileViewerActivityBase extends ActivityBase {
    protected FileInfo fileInfo;
    protected FileService fileService;
    protected FileIOHelper fileIOHelper;
    protected DialogFactory dialogFactory;
    protected File saveDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fileService = new FileService(DatabaseProvider.getInstance(getApplicationContext()));
        fileIOHelper = new FileIOHelper(fileService);
        dialogFactory = new DialogFactory(this);

        fileInfo = (FileInfo) getIntent().getSerializableExtra("fileInfo");

        if (fileInfo == null) {
            throw new RuntimeException("File info not provided");
        }

        setupActionBar();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(fileInfo.getName());
        }
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
        }

        if (id == R.id.saveImageButton) {
            saveFileOnClick();
            return true;
        }

        if (id == R.id.deleteImageButton) {
            deleteFileOnClick();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void saveFileOnClick() {
        File destFile = new File(saveDir, fileInfo.getName());

        Runnable task = () -> fileIOHelper.writeToFile(fileInfo.getId(), destFile);
        Runnable post = () -> showToastMessage(this,
                getString(R.string.saved_to, destFile.getAbsolutePath()),
                Toast.LENGTH_LONG);

        DialogInterface.OnClickListener onConfirm = (d, w) -> {
            if (destFile.exists()) {
                dialogFactory.showOverwriteDialog(() ->
                        runWithProgressDialog(task, post));
            } else {
                runWithProgressDialog(task, post);
            }
        };

        dialogFactory.showConfirmationDialog(
                R.layout.dialog_are_you_sure,
                R.string.warning,
                R.string.save,
                onConfirm
        );
    }

    protected void deleteFileOnClick() {
        Runnable task = () -> fileService.delete(fileInfo.getId());
        Runnable post = this::returnToListActivity;

        dialogFactory.showConfirmationDialog(
                R.layout.dialog_action_cannot_be_undo,
                R.string.warning,
                R.string.delete,
                (dialog, which) -> runWithProgressDialog(task, post)
        );
    }

    protected void runWithProgressDialog(Runnable backgroundTask, @Nullable Runnable afterUi) {
        AlertDialog dialog = dialogFactory.buildThemedDialog(R.layout.progress_dialog_deleting);
        dialog.setCancelable(false);
        dialog.show();

        newSingleThreadExecutor().execute(() -> {
            try {
                backgroundTask.run();
            } finally {
                runOnUiThread(() -> {
                    dialog.dismiss();
                    if (afterUi != null) {
                        afterUi.run();
                    }
                });
            }
        });
    }

    protected void returnToListActivity() {
        Intent intent = new Intent(getApplicationContext(), FilesListActivity.class)
                .putExtra("noteId", fileInfo.getNoteId())
                .putExtra("modified", true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivity(intent);
    }

    protected File dropToCache() {
        return fileIOHelper.dropToCache(fileInfo, getCacheDir());
    }
}
