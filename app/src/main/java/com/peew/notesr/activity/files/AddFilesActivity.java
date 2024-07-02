package com.peew.notesr.activity.files;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activity.AppCompatActivityExtended;
import com.peew.notesr.component.AssignmentsManager;
import com.peew.notesr.model.FileInfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddFilesActivity extends AppCompatActivityExtended {
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
                addFilesCallback());

        Intent intent = new Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT);

        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        resultLauncher.launch(Intent.createChooser(intent, getString(R.string.choose_files)));
    }

    @Override
    public void finish() {
        Intent intent = new Intent(App.getContext(), AssignmentsListActivity.class);

        intent.putExtra("note_id", noteId);
        startActivity(intent);

        super.finish();
    }

    private ActivityResultCallback<ActivityResult> addFilesCallback() {
        return result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                if (result.getData() != null) {
                    addFiles(result.getData());
                } else {
                    throw new RuntimeException("Activity result is 'OK', but data not provided");
                }
            }

            finish();
        };
    }

    private void addFiles(Intent data) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        AlertDialog progressDialog = createProgressDialog();

        Map<FileInfo, InputStream> files = new HashMap<>();

        getFilesUri(data).forEach(uri -> {
            String filename = getFileName(getCursor(uri));
            String type = getMimeType(filename);

            long size = getFileSize(getCursor(uri));

            FileInfo fileInfo = new FileInfo(noteId, size, filename, type);

            try {
                InputStream stream = getContentResolver().openInputStream(uri);
                files.put(fileInfo, stream);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        executor.execute(() -> {
            handler.post(progressDialog::show);

            AssignmentsManager manager = App.getAppContainer().getAssignmentsManager();

            files.forEach((info, stream) -> {
                Long fileId = manager.saveInfo(info);

                try {
                    manager.saveData(fileId, stream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            progressDialog.dismiss();
        });
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

    private Cursor getCursor(Uri uri) {
        Cursor cursor = App.getContext()
                .getContentResolver()
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

    private String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);

        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        return type;
    }

    private AlertDialog createProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setView(R.layout.progress_dialog_adding).setCancelable(false);

        return builder.create();
    }
}