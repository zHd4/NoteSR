package com.peew.notesr.activity.files;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activity.AppCompatActivityExtended;
import com.peew.notesr.component.AssignmentsManager;

import java.util.ArrayList;
import java.util.List;

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
        resultLauncher.launch(Intent.createChooser(intent, getString(R.string.choose_file)));
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

                    try {
                        addFiles(filesUri);
                    } catch (OutOfMemoryError e) {
                        showToastMessage(getResources()
                                .getString(R.string.failed_to_add_all_files_at_once), Toast.LENGTH_LONG);
                        Log.e("addFiles", e.toString(), e);
                    }

                } else {
                    throw new RuntimeException("Activity result is 'OK', but data not provided");
                }
            }

            finish();
        };
    }

    private void addFiles(List<Uri> filesUri) {
        AssignmentsManager manager = App.getAppContainer().getAssignmentsManager();
        filesUri.forEach(uri -> manager.save(noteId, uri));
    }

}