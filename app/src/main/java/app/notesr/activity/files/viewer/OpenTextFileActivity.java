package app.notesr.activity.files.viewer;

import android.os.Bundle;
import android.os.Environment;
import android.widget.EditText;
import app.notesr.App;
import app.notesr.R;
import app.notesr.service.FilesService;

public class OpenTextFileActivity extends FileViewerActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_text_file);

        saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        loadText();
    }

    private void loadText() {
        FilesService filesService = App.getAppContainer().getFilesService();
        EditText field = findViewById(R.id.textFileField);

        String content = new String(filesService.read(fileInfo.getId()));

        field.setText(content);
    }
}