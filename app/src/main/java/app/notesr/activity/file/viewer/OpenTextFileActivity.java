package app.notesr.activity.file.viewer;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import android.os.Bundle;
import android.os.Environment;
import android.widget.EditText;

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
                String content = new String(fileService.read(fileInfo.getId()));

                runOnUiThread(() -> {
                    EditText field = findViewById(R.id.textFileField);
                    field.setText(content);
                });
            } catch (IOException | DecryptionFailedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
