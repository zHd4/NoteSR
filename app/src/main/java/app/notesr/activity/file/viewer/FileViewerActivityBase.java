package app.notesr.activity.file.viewer;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import static app.notesr.core.util.ActivityUtils.showToastMessage;
import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import app.notesr.activity.DialogFactory;
import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.file.helper.FileIOHelper;
import app.notesr.activity.file.FilesListActivity;
import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.FilesUtils;
import app.notesr.data.DatabaseProvider;
import app.notesr.data.model.FileInfo;
import app.notesr.service.file.FileService;

import java.io.File;
import java.io.IOException;

public class FileViewerActivityBase extends ActivityBase {
    protected FileInfo fileInfo;
    protected FileService fileService;
    protected FileIOHelper fileIOHelper;
    protected DialogFactory dialogFactory;
    protected File saveDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getApplicationContext();
        CryptoSecrets secrets = CryptoManagerProvider.getInstance(context).getSecrets();
        AesCryptor cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(secrets));

        fileService = new FileService(
                context,
                DatabaseProvider.getInstance(context),
                cryptor,
                new FilesUtils()
        );

        fileIOHelper = new FileIOHelper(new FilesUtils(), fileService);
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

    protected final void saveFileOnClick() {
        File destFile = new File(saveDir, fileInfo.getName());

        Runnable task = () -> fileIOHelper.exportFile(fileInfo.getId(), destFile);
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

    protected final void deleteFileOnClick() {
        Runnable task = () -> {
            try {
                fileService.delete(fileInfo.getId());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        Runnable post = this::returnToListActivity;

        dialogFactory.showConfirmationDialog(
                R.layout.dialog_action_cannot_be_undo,
                R.string.warning,
                R.string.delete,
                (dialog, which) -> runWithProgressDialog(task, post)
        );
    }

    protected final void runWithProgressDialog(Runnable backgroundTask, @Nullable Runnable afterUi) {
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

    protected final void returnToListActivity() {
        Intent intent = new Intent(getApplicationContext(), FilesListActivity.class)
                .putExtra("noteId", fileInfo.getNoteId())
                .putExtra("modified", true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivity(intent);
    }

    protected final File dropToCache() {
        return fileIOHelper.dropToCache(fileInfo, getCacheDir());
    }

    protected boolean isFileSizeAllowed(long fileSize) {
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long freeMemory = Runtime.getRuntime().maxMemory() - usedMemory;

        long maxAllowedSize = freeMemory / 4;

        if (fileSize > maxAllowedSize) {
            Log.w("OpenImageActivity", "File too large: " + fileSize
                    + " bytes, limit: " + maxAllowedSize);

            return false;
        }

        return true;
    }
}
