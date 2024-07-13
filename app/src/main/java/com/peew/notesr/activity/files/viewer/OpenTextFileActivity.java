package com.peew.notesr.activity.files.viewer;

import android.os.Bundle;
import android.os.Environment;
import android.widget.EditText;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.manager.AssignmentsManager;

public class OpenTextFileActivity extends FileViewerActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_text_file);

        saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        loadText();
    }

    private void loadText() {
        AssignmentsManager assignmentsManager = App.getAppContainer().getAssignmentsManager();
        EditText field = findViewById(R.id.text_file_field);

        String content = new String(assignmentsManager.read(fileInfo.getId()));

        field.setText(content);
    }
}