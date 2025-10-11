package app.notesr.file.activity.viewer;

import android.graphics.Matrix;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

public final class ZoomableImageTouchListener implements View.OnTouchListener {

    private static final float MAX_SCALE = 5f;

    private float minScale = 1f;

    private final Matrix matrix = new Matrix();
    private final float[] matrixValues = new float[9];

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    private final ImageView imageView;
    private boolean initialized = false;

    public ZoomableImageTouchListener(ImageView imageView) {
        this.imageView = imageView;

        scaleDetector = new ScaleGestureDetector(imageView.getContext(), new ScaleListener());
        gestureDetector = new GestureDetector(imageView.getContext(), new GestureListener());

        imageView.addOnLayoutChangeListener((v, left, top, right, bottom,
                                             oldLeft, oldTop, oldRight,
                                             oldBottom) -> {

            if (!initialized && imageView.getDrawable() != null
                    && imageView.getWidth() > 0 && imageView.getHeight() > 0) {
                initImagePosition();
                imageView.setImageMatrix(matrix);

                initialized = true;
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        imageView.setImageMatrix(matrix);

        if (event.getAction() == MotionEvent.ACTION_UP) {
            v.performClick();
        }

        return true;
    }

    private void initImagePosition() {
        if (imageView.getDrawable() == null) {
            return;
        }

        float drawableWidth = imageView.getDrawable().getIntrinsicWidth();
        float drawableHeight = imageView.getDrawable().getIntrinsicHeight();

        float viewWidth = imageView.getWidth();
        float viewHeight = imageView.getHeight();

        if (drawableWidth == 0 || drawableHeight == 0) {
            return;
        }

        float scale = Math.min(viewWidth / drawableWidth, viewHeight / drawableHeight);

        if (scale > 1f) {
            scale = 1f;
        }

        float dx = (viewWidth - drawableWidth * scale) / 2f;
        float dy = (viewHeight - drawableHeight * scale) / 2f;

        matrix.setScale(scale, scale);
        matrix.postTranslate(dx, dy);

        minScale = scale;
    }

    private final class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();

            matrix.getValues(matrixValues);
            float currentScale = matrixValues[Matrix.MSCALE_X];

            float newScale = currentScale * scaleFactor;

            if (newScale < minScale) {
                scaleFactor = minScale / currentScale;
            }

            if (newScale > MAX_SCALE) {
                scaleFactor = MAX_SCALE / currentScale;
            }

            matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            fixTranslation();
            return true;
        }
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2,
                                float distanceX, float distanceY) {

            matrix.postTranslate(-distanceX, -distanceY);
            fixTranslation();
            return true;
        }
    }

    private void fixTranslation() {
        matrix.getValues(matrixValues);

        float transX = matrixValues[Matrix.MTRANS_X];
        float transY = matrixValues[Matrix.MTRANS_Y];
        float scaleX = matrixValues[Matrix.MSCALE_X];
        float scaleY = matrixValues[Matrix.MSCALE_Y];

        float viewWidth = imageView.getWidth();
        float viewHeight = imageView.getHeight();
        float drawableWidth = imageView.getDrawable().getIntrinsicWidth() * scaleX;
        float drawableHeight = imageView.getDrawable().getIntrinsicHeight() * scaleY;

        float maxTransX = 0f;
        float maxTransY = 0f;

        float minTransX = viewWidth - drawableWidth;
        float minTransY = viewHeight - drawableHeight;

        if (drawableWidth < viewWidth) {
            float offsetX = (viewWidth - drawableWidth) / 2f;

            minTransX = offsetX;
            maxTransX = offsetX;
        }

        if (drawableHeight < viewHeight) {
            float offsetY = (viewHeight - drawableHeight) / 2f;

            minTransY = offsetY;
            maxTransY = offsetY;
        }

        float clampedX = Math.min(Math.max(transX, minTransX), maxTransX);
        float clampedY = Math.min(Math.max(transY, minTransY), maxTransY);

        matrixValues[Matrix.MTRANS_X] = clampedX;
        matrixValues[Matrix.MTRANS_Y] = clampedY;

        matrix.setValues(matrixValues);
    }
}
