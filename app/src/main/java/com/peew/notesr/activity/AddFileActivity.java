package com.peew.notesr.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.peew.notesr.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class AddFileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_file);

        Intent intent = new Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT);

        ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                addFileCallback());

        resultLauncher.launch(Intent.createChooser(intent, getString(R.string.choose_file)));
    }

    private ActivityResultCallback<ActivityResult> addFileCallback() {
        return result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Uri dumpUri = Objects.requireNonNull(result.getData()).getData();

                try {
                    assert dumpUri != null;

                    try (InputStream stream = getContentResolver().openInputStream(dumpUri)) {
                        byte[] data = new byte[Objects.requireNonNull(stream).available()];

                        int dataSize = stream.read(data);
                        assert dataSize == data.length;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            finish();
        };
    }
}