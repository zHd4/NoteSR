package com.peew.notesr.activity.files.viewer;

import android.os.Bundle;
import com.peew.notesr.R;

public class OpenFileActivity extends FileViewerActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_file);
    }
}