package com.peew.notesr.activity.files;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activity.AppCompatActivityExtended;
import com.peew.notesr.crypto.FilesCrypt;
import com.peew.notesr.db.notes.tables.FilesTable;
import com.peew.notesr.model.File;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AddFileActivity extends AppCompatActivityExtended {
    private long noteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_file);

        noteId = getIntent().getLongExtra("note_id", -1);

        if (noteId == -1) {
            throw new RuntimeException("Note id didn't provided");
        }

        ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                addFileCallback());

        Intent intent = new Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT);

        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        resultLauncher.launch(Intent.createChooser(intent, getString(R.string.choose_file)));
    }

    @Override
    public void finish() {
        Intent intent = new Intent(App.getContext(), AssignmentsListActivity.class);

        intent.putExtra("note_id", noteId);
        startActivity(intent);

        super.finish();
    }

    private ActivityResultCallback<ActivityResult> addFileCallback() {
        return result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                if (result.getData() != null) {
                    List<Uri> filesUri = new ArrayList<>();

                    if (result.getData().getClipData() != null) {
                        ClipData clipData = result.getData().getClipData();
                        int filesCount = clipData.getItemCount();

                        for (int i = 0; i < filesCount; i++) {
                            filesUri.add(clipData.getItemAt(i).getUri());
                        }
                    } else {
                        filesUri.add(result.getData().getData());
                    }

                    addFiles(filesUri);
                } else {
                    throw new RuntimeException("Activity result is 'OK', but data not provided");
                }
            }

            finish();
        };
    }

    private void addFiles(List<Uri> filesUri) {
        FilesTable table = App.getAppContainer().getNotesDatabase().getFilesTable();

        filesUri.forEach(uri -> {
            String filename = getFileName(getCursor(uri));
            String type = getMimeType(filename);

            long size = getFileSize(getCursor(uri));
            byte[] data = getFileData(uri);

            File file = new File(noteId, filename, type, size, data);

            table.save(FilesCrypt.encrypt(file));
        });
    }

    private Cursor getCursor(Uri uri) {
        Cursor cursor = getContentResolver()
                .query(uri, null, null, null, null);

        if (cursor == null) {
            throw new RuntimeException(new NullPointerException("Cursor is null"));
        }

        return cursor;
    }

    private String getFileName(Cursor cursor) {
        try (cursor) {
            int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

            cursor.moveToFirst();
            return cursor.getString(index);
        }
    }

    private long getFileSize(Cursor cursor) {
        try (cursor) {
            int index = cursor.getColumnIndex(OpenableColumns.SIZE);

            cursor.moveToFirst();
            return cursor.getLong(index);
        }
    }

    private byte[] getFileData(Uri uri) {
        try (InputStream stream = getContentResolver().openInputStream(uri)) {
            byte[] data = new byte[Objects.requireNonNull(stream).available()];
            int result = stream.read(data);

            if (result == -1) {
                throw new RuntimeException("End of the stream has been reached");
            }

            return data;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);

        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        return type;
    }
}