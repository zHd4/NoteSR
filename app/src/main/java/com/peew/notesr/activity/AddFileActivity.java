package com.peew.notesr.activity;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.peew.notesr.R;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AddFileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_file);

        ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                addFileCallback());

        Intent intent = new Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT);

        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        resultLauncher.launch(Intent.createChooser(intent, getString(R.string.choose_file)));
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

    }
}