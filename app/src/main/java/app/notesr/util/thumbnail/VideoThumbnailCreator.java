package app.notesr.util.thumbnail;

import static java.util.UUID.randomUUID;

import android.content.Context;
import android.util.Log;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import app.notesr.util.Wiper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class VideoThumbnailCreator implements ThumbnailCreator {
    private static final String TAG = VideoThumbnailCreator.class.getName();

    public static final int WIDTH = 100;
    public static final int QUALITY = 5;
    public static final String THUMBNAIL_FORMAT = "webp";

    private final Context context;

    @Override
    public byte[] getThumbnail(File file) {
        String tempThumbnailFileName = randomUUID().toString() + "." + THUMBNAIL_FORMAT;
        File tempThumbnailFile = new File(context.getCacheDir(), tempThumbnailFileName);

        String command = "-i " + file.getAbsolutePath() + " -ss 00:00:01 -vframes 1 -vf scale="
                + WIDTH + ":-1 -q:v " + QUALITY + " " + tempThumbnailFile.getAbsolutePath();

        Log.i(TAG, "Command: " + command);
        FFmpegSession session = FFmpegKit.execute(command);

        if (ReturnCode.isSuccess(session.getReturnCode())) {
            try {
                byte[] thumbnailBytes = new byte[(int) tempThumbnailFile.length()];

                try (FileInputStream inputStream = new FileInputStream(tempThumbnailFile)) {
                    inputStream.read(thumbnailBytes);
                }

                Wiper.wipeFile(tempThumbnailFile);

                return thumbnailBytes;
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
                throw new RuntimeException(e);
            }
        } else {
            Log.e(TAG, "FFmpeg error: " + session.getFailStackTrace());
        }

        return null;
    }
}
