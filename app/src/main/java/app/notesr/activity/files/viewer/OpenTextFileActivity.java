package app.notesr.activity.files.viewer;

import android.os.Bundle;
import android.os.Environment;
import android.widget.EditText;
import app.notesr.App;
import app.notesr.R;
import app.notesr.service.FileService;

public class OpenTextFileActivity extends FileViewerActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_text_file);

        saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        loadText();
    }

    private void loadText() {
        FileService fileService = App.getAppContainer().getFileService();
        EditText field = findViewById(R.id.textFileField);

        String content = new String(fileService.read(fileInfo.getId()));

        field.setText(content);
    }
}