package com.peew.notesr.activity.files.viewer;

import android.os.Bundle;
import android.os.Environment;
import android.widget.VideoView;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.manager.AssignmentsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

public class OpenVideoActivity extends FileViewerActivityBase {

    private static final Random RANDOM = new Random();

    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_video);

        saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        loadFileInfo();
    }

    private void dropToCache() {
        try {
            String extension = new LinkedList<>(Arrays.asList(fileInfo.getName().split("\\."))).getLast();

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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}