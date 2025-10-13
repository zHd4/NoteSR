package app.notesr.util.thumbnail;

import android.net.Uri;

import java.io.IOException;

public interface ThumbnailCreator {
    byte[] getThumbnail(Uri uri) throws IOException;
}
