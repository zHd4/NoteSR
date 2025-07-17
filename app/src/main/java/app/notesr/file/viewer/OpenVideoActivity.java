package app.notesr.file.viewer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.appcompat.app.AlertDialog;
import app.notesr.App;
import app.notesr.R;
import app.notesr.db.service.dao.TempFileDao;
import app.notesr.model.TempFile;
import app.notesr.service.android.CacheCleanerAndroidService;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenVideoActivity extends MediaFileViewerActivityBase {

    private ScaleGestureDetector scaleGestureDetector;
    private VideoView videoView;
    private File videoFile;
    private boolean playing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_video);

        saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);

        videoView = findViewById(R.id.openVideoView);
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener(videoView));

        videoView.setOnClickListener(null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!playing) {
            TextView label = findViewById(R.id.tapToPlayLabel);

            videoView.setVisibility(View.VISIBLE);
            label.setVisibility(View.INVISIBLE);

            loadVideo();
            startForegroundService(new Intent(getApplicationContext(), CacheCleanerAndroidService.class));

            playing = true;
        } else {
            scaleGestureDetector.onTouchEvent(motionEvent);
        }

        return true;
    }

    private void loadVideo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setView(R.layout.progress_dialog_loading).setCancelable(false);

        AlertDialog progressDialog = builder.create();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        TempFileDao tempFileTable = App.getAppContainer()
                .getServicesDB()
                .getDao(TempFileDao.class);

        executor.execute(() -> {
            runOnUiThread(progressDialog::show);
            videoFile = dropToCache(fileInfo);

            if (!isThumbnailSet()) {
                setThumbnail(videoFile);
            }

            runOnUiThread(progressDialog::dismiss);
            runOnUiThread(() -> {
                Uri videoUri = Uri.parse(videoFile.getAbsolutePath());
                TempFile tempFile = new TempFile(videoUri);

                tempFileTable.save(tempFile);

                setVideo(videoUri);
                videoView.start();
            });
        });
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
}