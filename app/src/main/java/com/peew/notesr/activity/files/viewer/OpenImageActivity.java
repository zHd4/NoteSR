package com.peew.notesr.activity.files.viewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;

import com.peew.notesr.R;
import com.peew.notesr.model.File;

public class OpenImageActivity extends FileViewerActivityBase {
    private File imageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_image);

        imageFile = (File) getIntent().getSerializableExtra("file");

        if (imageFile == null) {
            throw new RuntimeException("Image file not provided");
        }

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(imageFile.getName());

        setImage();
    }

    private void setImage() {
        byte[] imageBytes = imageFile.getData();

        ImageView imageView = findViewById(R.id.assigned_image_view);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        imageView.setImageBitmap(bitmap);
    }
}