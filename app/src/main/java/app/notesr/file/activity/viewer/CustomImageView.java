package app.notesr.file.activity.viewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

class CustomImageView extends View {

    private final Rect regionRect = new Rect();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private BitmapRegionDecoder decoder;
    private int imageWidth, imageHeight;

    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PACKAGE)
    private float scale = 1f;

    @Getter(AccessLevel.PACKAGE)
    private float minScale;

    @Getter(AccessLevel.PACKAGE)
    private final float maxScale = 5f;

    @Getter(AccessLevel.PACKAGE)
    private float translateX = 0f;

    @Getter(AccessLevel.PACKAGE)
    private float translateY = 0f;

    private Bitmap currentBitmap;

    private final ScaleGestureDetector scaleDetector;

    @Getter(AccessLevel.PACKAGE)
    private final GestureDetector gestureDetector;

    private boolean firstDraw = true;

    public CustomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public void setImage(File file) throws IOException {
        decoder = BitmapRegionDecoder.newInstance(file.getAbsolutePath(), false);
        initDecoder();
    }

    public void setImage(InputStream inputStream) throws IOException {
        decoder = BitmapRegionDecoder.newInstance(inputStream, false);
        initDecoder();
    }

    private void initDecoder() {
        if (decoder == null) {
            return;
        }

        imageWidth = decoder.getWidth();
        imageHeight = decoder.getHeight();

        requestLayout();
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (firstDraw && decoder != null) {
            int viewW = getWidth();
            int viewH = getHeight();

            if (viewW > 0 && viewH > 0) {
                minScale = Math.min((float) viewW / imageWidth, (float) viewH / imageHeight);
                scale = minScale;

                translateX = (viewW - imageWidth * scale) / 2f;
                translateY = (viewH - imageHeight * scale) / 2f;

                loadRegion();
                firstDraw = false;
            }
        }

        if (currentBitmap != null) {
            canvas.save();
            canvas.translate(translateX, translateY);
            canvas.scale(scale, scale);
            canvas.drawBitmap(currentBitmap, regionRect.left, regionRect.top, paint);
            canvas.restore();
        }
    }

    private void loadRegion() {
        if (decoder == null) {
            return;
        }

        int viewW = getWidth();
        int viewH = getHeight();

        if (viewW == 0 || viewH == 0) {
            return;
        }

        float invScale = 1f / scale;

        int left = Math.max(0, (int) (-translateX * invScale));
        int top = Math.max(0, (int) (-translateY * invScale));

        int right = Math.min(imageWidth, (int) ((viewW - translateX) * invScale));
        int bottom = Math.min(imageHeight, (int) ((viewH - translateY) * invScale));

        if (left >= right || top >= bottom) {
            return;
        }

        regionRect.set(left, top, right, bottom);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.RGB_565;

        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            currentBitmap.recycle();
        }

        currentBitmap = decoder.decodeRegion(regionRect, opts);

        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = scaleDetector.onTouchEvent(event);
        handled = gestureDetector.onTouchEvent(event) || handled;

        if (event.getAction() == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_CANCEL) {
            fixBounds();
            loadRegion();
        }

        return handled || super.onTouchEvent(event);
    }

    private void fixBounds() {
        if (decoder == null) return;

        float scaledW = imageWidth * scale;
        float scaledH = imageHeight * scale;
        int viewW = getWidth();
        int viewH = getHeight();

        if (scaledW <= viewW) {
            translateX = (viewW - scaledW) / 2f;
        } else {
            translateX = Math.min(translateX, 0);
            translateX = Math.max(translateX, viewW - scaledW);
        }

        if (scaledH <= viewH) {
            translateY = (viewH - scaledH) / 2f;
        } else {
            translateY = Math.min(translateY, 0);
            translateY = Math.max(translateY, viewH - scaledH);
        }
    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2,
                                float distanceX, float distanceY) {
            translateX -= distanceX;
            translateY -= distanceY;

            invalidate();
            return true;
        }

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            float targetScale;

            if (scale <= minScale + 0.01f) {
                targetScale = maxScale / 2f;
            } else {
                targetScale = minScale;
            }

            float focusX = e.getX();
            float focusY = e.getY();

            float prevScale = scale;
            scale = targetScale;

            translateX = focusX - (focusX - translateX) * (scale / prevScale);
            translateY = focusY - (focusY - translateY) * (scale / prevScale);

            fixBounds();
            loadRegion();
            return true;
        }
    }

    class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float prevScale = scale;
            scale *= detector.getScaleFactor();
            scale = Math.max(minScale, Math.min(scale, maxScale));

            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            translateX = focusX - (focusX - translateX) * (scale / prevScale);
            translateY = focusY - (focusY - translateY) * (scale / prevScale);

            invalidate();
            return true;
        }
    }
}
