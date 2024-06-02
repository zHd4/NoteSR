package com.peew.notesr.ui.manage.notes;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.db.notes.NotesImportResult;
import com.peew.notesr.db.notes.NotesImporter;
import com.peew.notesr.ui.ExtendedAppCompatActivity;
import com.peew.notesr.ui.MainActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImportNotesActivity extends ExtendedAppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_notes);

        ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                getImportCallback());

        Intent intent = new Intent()
                .setType("application/x-trash")
                .setAction(Intent.ACTION_GET_CONTENT);

        resultLauncher.launch(Intent.createChooser(intent, getString(R.string.select_a_dump)));
    }

    private ActivityResultCallback<ActivityResult> getImportCallback() {
        return result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Uri dumpUri = Objects.requireNonNull(result.getData()).getData();

                try {
                    byte[] data = getDumpData(dumpUri);

                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            this, R.style.AlertDialogTheme);

                    builder.setView(R.layout.progress_dialog_importing).setCancelable(false);
                    AlertDialog progressDialog = builder.create();

                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Handler handler = new Handler(Looper.getMainLooper());

                    executor.execute(() -> {
                        handler.post(progressDialog::show);
                        proceedImport(data);

                        progressDialog.dismiss();
                        finish();
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private void proceedImport(byte[] dumpBytes) {
        NotesImporter importer = new NotesImporter(this);
        NotesImportResult importResult = importer.importDump(dumpBytes);

        runOnUiThread(() -> {
            showToastMessage(getResultMessage(importResult), Toast.LENGTH_SHORT);
            startActivity(new Intent(App.getContext(), MainActivity.class));
        });
    }

    /** @noinspection ResultOfMethodCallIgnored*/
    private byte[] getDumpData(Uri dumpUri) throws IOException {
        try (InputStream stream = getContentResolver().openInputStream(dumpUri)) {
            byte[] data = new byte[Objects.requireNonNull(stream).available()];
            stream.read(data);

            return data;
        }
    }

    private String getResultMessage(NotesImportResult result) {
        switch (result) {
            case SUCCESS -> {
                return getString(R.string.imported);
            }
            case INCOMPATIBLE_VERSION -> {
                return getString(R.string.incompatible_file_version);
            }
            case INVALID_DUMP -> {
                return getString(R.string.cannot_decrypt_file);
            }
            default -> throw new IllegalArgumentException();
        }
    }
}