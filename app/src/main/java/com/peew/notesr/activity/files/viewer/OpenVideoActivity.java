package com.peew.notesr.activity.files.viewer;

import android.os.Bundle;
import android.widget.VideoView;
import com.peew.notesr.R;

public class OpenVideoActivity extends FileViewerActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_video);
    }
}