package app.notesr.file.activity.viewer;

import android.os.Bundle;
import app.notesr.R;

public class OpenUnknownFileActivity extends FileViewerActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_unknown_file);
    }
}