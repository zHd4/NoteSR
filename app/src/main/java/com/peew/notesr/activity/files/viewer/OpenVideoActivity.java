package com.peew.notesr.activity.files.viewer;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.VideoView;
import androidx.appcompat.app.ActionBar;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.db.services.tables.TempFilesTable;
import com.peew.notesr.manager.AssignmentsManager;
import com.peew.notesr.model.FileInfo;
import com.peew.notesr.model.TempFile;
import com.peew.notesr.tools.FileManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
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
        videoFile = dropToCache(fileInfo);

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener(videoView));

        TempFilesTable tempFilesTable = App.getAppContainer()
                .getServicesDB()
                .getTable(TempFilesTable.class);

        Uri videoUri = Uri.parse(videoFile.getAbsolutePath());
        TempFile tempFile = new TempFile(videoUri);

        tempFilesTable.save(tempFile);

        setVideo(videoUri);
        videoView.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        scaleGestureDetector.onTouchEvent(motionEvent);
        return true;
    }

    private void setVideo(Uri uri) {
        videoView.setVideoURI(uri);

        MediaController mediaController = new MediaController(this);

        mediaController.setAnchorView(videoView);
        mediaController.setMediaPlayer(videoView);

        DisplayMetrics metrics = new DisplayMetrics();
        ViewGroup.LayoutParams params = videoView.getLayoutParams();

        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        params.width = metrics.widthPixels;
        params.height = metrics.heightPixels;

        videoView.setMediaController(mediaController);
        videoView.setLayoutParams(params);
    }

    private File dropToCache(FileInfo fileInfo) {
        try {
            String extension = FileManager.getFileExtension(fileInfo.getName());

            File tempDir = getCacheDir();
            File tempVideo = File.createTempFile(String.valueOf(RANDOM.nextLong()), "." + extension, tempDir);

            AssignmentsManager assignmentsManager = App.getAppContainer().getAssignmentsManager();
            FileOutputStream stream = new FileOutputStream(tempVideo);

            assignmentsManager.read(fileInfo.getId(), chunk -> {
                try {
                    stream.write(chunk);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            stream.close();
            return tempVideo;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}