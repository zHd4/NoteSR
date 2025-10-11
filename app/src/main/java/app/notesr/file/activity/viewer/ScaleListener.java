package app.notesr.file.activity.viewer;

import android.view.ScaleGestureDetector;
import android.view.View;

public final class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

    private float scaleFactor = 1.0f;

    private final View view;

    public ScaleListener(View view) {
        this.view = view;
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        scaleFactor *= scaleGestureDetector.getScaleFactor();
        scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f));

        view.setScaleX(scaleFactor);
        view.setScaleY(scaleFactor);

        return true;
    }
}
