/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */
 
package app.notesr.activity.file.viewer;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import android.os.Bundle;
import android.os.Environment;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import app.notesr.R;
import app.notesr.core.security.exception.DecryptionFailedException;

public final class OpenTextFileActivity extends FileViewerActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_text_file);

        saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        loadText();
    }

    private void loadText() {
        newSingleThreadExecutor().execute(() -> {
            try {
                TextView errorMessageTextView = findViewById(R.id.error_message_text_view);

                if (!isFileSizeAllowed(fileInfo.getSize())) {
                    runOnUiThread(() ->
                            errorMessageTextView.setText(R.string.failed_to_load_the_file));
                    return;
                }

                String content = new String(fileService.read(fileInfo.getId()));

                runOnUiThread(() -> {
                    EditText field = findViewById(R.id.file_content_edit_text_view);
                    field.setText(content);
                });
            } catch (IOException | DecryptionFailedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
