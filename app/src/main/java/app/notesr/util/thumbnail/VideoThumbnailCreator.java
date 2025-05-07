package app.notesr.util.thumbnail;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class VideoThumbnailCreator implements ThumbnailCreator {
    private static final String TAG = VideoThumbnailCreator.class.getName();
    private static final Bitmap.CompressFormat THUMBNAIL_FORMAT = Bitmap.CompressFormat.WEBP;
    private static final int TIME_US = 1;
    private static final int QUALITY = 90;
    private static final int WIDTH = 200;

    @Override
    public byte[] getThumbnail(File file) {
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(file.getAbsolutePath());
            Bitmap original = retriever.getFrameAtTime(TIME_US);

            if (original == null) return null;

            int width = original.getWidth();
            int height = original.getHeight();

            if (width > WIDTH) {
                float scale = (float) WIDTH / width;
                int newHeight = Math.round(height * scale);

                Bitmap scaled = Bitmap.createScaledBitmap(original, WIDTH, newHeight, true);

                original.recycle();
                original = scaled;
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            original.compress(THUMBNAIL_FORMAT, QUALITY, stream);
            retriever.release();

            return stream.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            throw new RuntimeException(e);
        }
    }
}
