package com.peew.notesr.activity.files.viewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import androidx.appcompat.app.ActionBar;
import com.peew.notesr.R;

public class OpenImageActivity extends FileViewerActivityBase {
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_image);

        imageView = findViewById(R.id.assigned_image_view);
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        loadFile();

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(file.getName());

        setImage();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        scaleGestureDetector.onTouchEvent(motionEvent);
        return true;
    }

    private void setImage() {
        byte[] imageBytes = file.getData();
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        imageView.setImageBitmap(bitmap);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            scaleFactor *= scaleGestureDetector.getScaleFactor();
            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f));
            imageView.setScaleX(scaleFactor);
            imageView.setScaleY(scaleFactor);
            return true;
        }
    }
}