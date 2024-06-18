package com.peew.notesr.activity.files.viewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.ActionBar;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.component.AssignmentsManager;
import com.peew.notesr.model.File;
import com.peew.notesr.model.FileInfo;

import java.io.IOException;

public class OpenImageActivity extends FileViewerActivityBase {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_image);

        //noinspection deprecation
        FileInfo imageInfo = (FileInfo) getIntent().getSerializableExtra("file_info");

        if (imageInfo == null) {
            throw new RuntimeException("Image info not provided");
        }

        try {
            AssignmentsManager manager = App.getAppContainer().getAssignmentsManager();

            file = new File(imageInfo);
            file.setData(manager.get(imageInfo.getId()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(file.getName());

        setImage();
    }

    private void setImage() {
        byte[] imageBytes = file.getData();

        ImageView imageView = findViewById(R.id.assigned_image_view);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        imageView.setImageBitmap(bitmap);
    }
}