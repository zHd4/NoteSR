package notesr.activity.files.viewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import notesr.App;
import notesr.R;

public class OpenImageActivity extends BaseFileViewerActivity {
    private ScaleGestureDetector scaleGestureDetector;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_image);

        saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        imageView = findViewById(R.id.assignedImageView);
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener(imageView));

        setImage();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        scaleGestureDetector.onTouchEvent(motionEvent);
        return true;
    }

    private void setImage() {
        byte[] imageBytes = App.getAppContainer()
                .getAssignmentsManager()
                .read(fileInfo.getId());

        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        imageView.setImageBitmap(bitmap);
    }
}