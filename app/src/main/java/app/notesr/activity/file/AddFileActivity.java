package app.notesr.activity.file;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import app.notesr.R;
import app.notesr.ActivityBase;
import app.notesr.data.DatabaseProvider;
import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.service.file.FileService;
import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.FilesUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class AddFileActivity extends ActivityBase {
    private FileService fileService;
    private String noteId;
    private boolean noteModified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_file);

        Context context = getApplicationContext();
        CryptoSecrets secrets = CryptoManagerProvider.getInstance(context).getSecrets();
        AesCryptor cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(secrets));

        fileService = new FileService(
                getApplicationContext(),
                DatabaseProvider.getInstance(getApplicationContext()),
                cryptor,
                new FilesUtils()
        );

        noteId = getIntent().getStringExtra("noteId");

        if (noteId == null) {
            throw new RuntimeException("Note id didn't provided");
        }

        ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                addFilesCallback());

        Intent intent = new Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT);

        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        resultLauncher.launch(Intent.createChooser(intent, getString(R.string.choose_files)));
    }

    @Override
    public void finish() {
        Intent intent = new Intent(getApplicationContext(), FilesListActivity.class);

        intent.putExtra("noteId", noteId);
        intent.putExtra("modified", noteModified);
        startActivity(intent);

        super.finish();
    }

    private ActivityResultCallback<ActivityResult> addFilesCallback() {
        return result -> {
            int resultCode = result.getResultCode();

            if (resultCode == Activity.RESULT_OK) {
                if (result.getData() != null) {
                    addFiles(result.getData());
                } else {
                    throw new RuntimeException("Activity result is 'OK', but data not provided");
                }
            } else {
                finish();
            }
        };
    }

    private void addFiles(Intent data) {
        Dialog progressDialog = createProgressDialog();

        newSingleThreadExecutor().execute(() -> {
            runOnUiThread(progressDialog::show);

            try {
                fileService.saveFiles(noteId, getFilesUri(data));
            } catch (IOException | DecryptionFailedException e) {
                throw new RuntimeException(e);
            }

            runOnUiThread(() -> {
                progressDialog.dismiss();
                onFilesAddedCallback();
            });
        });
    }

    private void onFilesAddedCallback() {
        noteModified = true;
        finish();
    }

    private List<Uri> getFilesUri(Intent data) {
        List<Uri> result = new ArrayList<>();

        if (data.getClipData() != null) {
            ClipData clipData = data.getClipData();
            int filesCount = clipData.getItemCount();

            for (int i = 0; i < filesCount; i++) {
                result.add(clipData.getItemAt(i).getUri());
            }
        } else {
            result.add(data.getData());
        }

        return result;
    }

    private AlertDialog createProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setView(R.layout.progress_dialog_adding).setCancelable(false);

        return builder.create();
    }
}
