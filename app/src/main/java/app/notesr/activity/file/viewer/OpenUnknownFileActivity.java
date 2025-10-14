package app.notesr.activity.file.viewer;

import android.os.Bundle;
import app.notesr.R;

public final class OpenUnknownFileActivity extends FileViewerActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_unknown_file);
    }
}
