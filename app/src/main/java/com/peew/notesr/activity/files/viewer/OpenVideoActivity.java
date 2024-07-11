package com.peew.notesr.activity.files.viewer;

import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.VideoView;
import com.peew.notesr.R;

import java.io.File;
import java.util.Random;

public class OpenVideoActivity extends FileViewerActivityBase {

    private static final Random RANDOM = new Random();

    private ScaleGestureDetector scaleGestureDetector;
    private VideoView videoView;
    private File videoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_video);

        saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        videoView = findViewById(R.id.open_video_view);

        videoView.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        scaleGestureDetector.onTouchEvent(motionEvent);
        return true;
    }
}