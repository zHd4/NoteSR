package app.notesr.file.activity.viewer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;

@RunWith(AndroidJUnit4.class)
public class CustomImageViewTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testLoadImageFromFileCentered() throws IOException {
        File file = new File(context.getFilesDir(), "test_image.jpg");
        Bitmap bitmap = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888);
        FileOutputStream outputStream = new FileOutputStream(file);

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        outputStream.close();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            CustomImageView view = new CustomImageView(context, null);

            view.measure(
                    View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY)
            );

            view.layout(0, 0, 500, 500);

            try {
                view.setImage(file);
                view.onDraw(new Canvas());

                float expectedTranslateX = (500 - 100 * view.getMinScale()) / 2f;
                float expectedTranslateY = (500 - 50 * view.getMinScale()) / 2f;

                assertEquals(expectedTranslateX, view.getTranslateX(), 0.01f);
                assertEquals(expectedTranslateY, view.getTranslateY(), 0.01f);

                Files.delete(file.toPath());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Test
    public void testLoadImageFromInputStream() {
        Bitmap bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream bitmapOutputStream = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bitmapOutputStream);
        InputStream inputStream = new ByteArrayInputStream(bitmapOutputStream.toByteArray());

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            CustomImageView view = new CustomImageView(context, null);

            view.measure(
                    View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY)
            );

            view.layout(0, 0, 400, 400);

            try {
                view.setImage(inputStream);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            view.onDraw(new Canvas());

            float expectedTranslateX = (400 - 200 * view.getMinScale()) / 2f;
            float expectedTranslateY = (400 - 200 * view.getMinScale()) / 2f;

            assertEquals(expectedTranslateX, view.getTranslateX(), 0.01f);
            assertEquals(expectedTranslateY, view.getTranslateY(), 0.01f);
        });
    }

    @Test
    public void testScroll() {
        Bitmap bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream bitmapOutputStream = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bitmapOutputStream);
        InputStream inputStream = new ByteArrayInputStream(bitmapOutputStream.toByteArray());

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            CustomImageView view = new CustomImageView(context, null);

            view.measure(
                    View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY)
            );

            view.layout(0, 0, 400, 400);

            try {
                view.setImage(inputStream);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            view.onDraw(new Canvas());

            float oldTranslateX = view.getTranslateX();
            float oldTranslateY = view.getTranslateY();

            MotionEvent e1 = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE,
                    200, 200, 0);

            MotionEvent e2 = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE,
                    150, 150, 0);

            view.onTouchEvent(e1);
            view.onTouchEvent(e2);

            assertNotEquals(oldTranslateX, view.getTranslateX());
            assertNotEquals(oldTranslateY, view.getTranslateY());
        });
    }

    @Test
    public void testPinchToZoomAndDoubleTap() {
        Bitmap bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream bitmapOutputStream = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bitmapOutputStream);
        InputStream inputStream = new ByteArrayInputStream(bitmapOutputStream.toByteArray());

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            CustomImageView view = new CustomImageView(context, null);

            view.measure(
                    View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY)
            );

            view.layout(0, 0, 400, 400);

            try {
                view.setImage(inputStream);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            view.onDraw(new Canvas());

            float initialScale = view.getScale();

            view.new ScaleListener().onScale(new ScaleGestureDetector(context,
                    view.new ScaleListener()) {
                @Override
                public float getScaleFactor() {
                    return 2f;
                }
            });

            assertTrue(view.getScale() > initialScale);

            MotionEvent doubleTapEvent = MotionEvent.obtain(0, 0,
                    MotionEvent.ACTION_DOWN, 200, 200, 0);
            view.new GestureListener().onDoubleTap(doubleTapEvent);

            assertTrue(view.getScale() == view.getMaxScale() / 2f
                    || view.getScale() == view.getMinScale());
        });
    }
}
