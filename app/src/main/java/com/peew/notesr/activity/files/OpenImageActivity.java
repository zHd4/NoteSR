package com.peew.notesr.activity.files;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.peew.notesr.R;
import com.peew.notesr.model.File;

public class OpenImageActivity extends AppCompatActivity {
    private File imageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_image);

        imageFile = (File) getIntent().getSerializableExtra("image");

        if (imageFile == null) {
            throw new RuntimeException("Image file not provided");
        }
    }
}