package com.peew.notesr.activity.files;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.peew.notesr.tools.FileExifDataResolver;

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
            FileExifDataResolver resolver = new FileExifDataResolver(uri);

            String filename = resolver.getFileName();
            String type = resolver.getMimeType();

            long size = resolver.getFileSize();

            FileInfo fileInfo = new FileInfo();

            fileInfo.setNoteId(noteId);
            fileInfo.setSize(size);
            fileInfo.setName(filename);
            fileInfo.setType(type);

            try {
                InputStream stream = getContentResolver().openInputStream(uri);
                map.put(fileInfo, stream);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        return map;
    }

    private AlertDialog createProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setView(R.layout.progress_dialog_adding).setCancelable(false);

        return builder.create();
    }
}