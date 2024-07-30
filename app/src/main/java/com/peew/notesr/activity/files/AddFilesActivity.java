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
import androidx.appcompat.app.AlertDialog;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activity.ExtendedAppCompatActivity;
import com.peew.notesr.manager.AssignmentsManager;
import com.peew.notesr.model.FileInfo;
import com.peew.notesr.tools.FileManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddFilesActivity extends ExtendedAppCompatActivity {
    private long noteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_file);

        noteId = getIntent().getLongExtra("noteId", -1);

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

        intent.putExtra("noteId", noteId);
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
            } else if (resultCode == Activity.RESULT_CANCELED) {
                finish();
            }
        };
    }

    private void addFiles(Intent data) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AlertDialog progressDialog = createProgressDialog();

        Map<FileInfo, InputStream> filesMap = getFilesMap(getFilesUri(data));

        executor.execute(() -> {
            runOnUiThread(progressDialog::show);

            AssignmentsManager manager = App.getAppContainer().getAssignmentsManager();

            filesMap.forEach((info, stream) -> {
                Long fileId = manager.saveInfo(info);

                try {
                    manager.saveData(fileId, stream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            runOnUiThread(() -> {
                progressDialog.dismiss();
                finish();
            });
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

    private Map<FileInfo, InputStream> getFilesMap(List<Uri> uris) {
        Map<FileInfo, InputStream> map = new HashMap<>();

        uris.forEach(uri -> {
            String filename = getFileName(getCursor(uri));
            String type = getMimeType(filename);

            long size = getFileSize(getCursor(uri));

            FileInfo fileInfo = new FileInfo(noteId, size, filename, type);

            try {
                InputStream stream = getContentResolver().openInputStream(uri);
                map.put(fileInfo, stream);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        return map;
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

    private String getMimeType(String filename) {
        String type = null;
        String extension = FileManager.getFileExtension(filename);

        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        return type;
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

    private AlertDialog createProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setView(R.layout.progress_dialog_adding).setCancelable(false);

        return builder.create();
    }
}