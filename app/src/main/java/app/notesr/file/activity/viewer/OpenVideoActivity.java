package app.notesr.file.activity.viewer;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import android.app.Dialog;
import android.content.Context;
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
import app.notesr.db.DatabaseProvider;
import app.notesr.cleaner.model.TempFile;
import app.notesr.cleaner.service.CacheCleanerAndroidService;
import app.notesr.cleaner.service.TempFileService;

import java.io.File;

public class OpenVideoActivity extends MediaFileViewerActivityBase {

    private ScaleGestureDetector scaleGestureDetector;
    private TempFileService tempFileService;
    private VideoView videoView;

    private boolean loading = false;
    private boolean playing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_video);

        saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);

        Context context = getApplicationContext();
        tempFileService = new TempFileService(DatabaseProvider.getInstance(context));

        videoView = findViewById(R.id.openVideoView);
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener(videoView));

        videoView.setOnClickListener(null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!loading && !playing) {
            loading = true;
            videoView.setVisibility(View.VISIBLE);

            TextView tapToPlayLabel = findViewById(R.id.tapToPlayLabel);
            tapToPlayLabel.setVisibility(View.INVISIBLE);

            AlertDialog.Builder builder = new AlertDialog.Builder(this,
                    R.style.AlertDialogTheme);
            builder.setView(R.layout.progress_dialog_loading).setCancelable(false);

            AlertDialog progressDialog = builder.create();

            newSingleThreadExecutor().execute(() -> {
                loadVideo(progressDialog);
                startPlayingVideo();
            });

        } else {
            scaleGestureDetector.onTouchEvent(motionEvent);
        }

        return true;
    }

    private void loadVideo(Dialog progressDialog) {
        runOnUiThread(progressDialog::show);
        File videoFile = dropToCache();

        if (!isThumbnailSet()) {
            setThumbnail(videoFile);
        }

        Uri videoUri = Uri.parse(videoFile.getAbsolutePath());

        TempFile tempFile = new TempFile();
        tempFile.setUri(videoUri);

        tempFileService.save(tempFile);

        runOnUiThread(progressDialog::dismiss);
        setVideo(videoUri);
    }

    private void startPlayingVideo() {
        runOnUiThread(() -> {
            videoView.start();

            if (!App.getContext().isServiceRunning(CacheCleanerAndroidService.class)) {
                startForegroundService(new Intent(getApplicationContext(),
                        CacheCleanerAndroidService.class));
            }

            loading = false;
            playing = true;
        });
    }

    private void setVideo(Uri uri) {
        runOnUiThread(() -> {
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
        });
    }
}