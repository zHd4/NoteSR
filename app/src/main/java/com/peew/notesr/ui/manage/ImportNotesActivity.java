package com.peew.notesr.ui.manage;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.peew.notesr.R;
import com.peew.notesr.db.notes.NotesImportResult;
import com.peew.notesr.db.notes.NotesImporter;
import com.peew.notesr.ui.ExtendedAppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

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
                Uri fileUri = Objects.requireNonNull(result.getData()).getData();

                try {
                    byte[] data = getDumpData(fileUri);

                    NotesImporter importer = new NotesImporter(this);
                    NotesImportResult importResult = importer.importDump(data);

                    if (importResult == NotesImportResult.INCOMPATIBLE_VERSION) {
                        showToastMessage(getString(R.string.incompatible_file_version),
                                Toast.LENGTH_SHORT);
                    } else if (importResult == NotesImportResult.INVALID_DUMP) {
                        showToastMessage(getString(R.string.invalid_file), Toast.LENGTH_SHORT);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            finish();
        };
    }

    /** @noinspection ResultOfMethodCallIgnored*/
    private byte[] getDumpData(Uri dumpUri) throws IOException {
        try (InputStream stream = getContentResolver().openInputStream(dumpUri)) {
            byte[] data = new byte[Objects.requireNonNull(stream).available()];
            stream.read(data);

            return data;
        }
    }
}