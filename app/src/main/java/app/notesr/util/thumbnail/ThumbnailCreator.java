package app.notesr.util.thumbnail;

import java.io.File;
import java.io.FileNotFoundException;

public interface ThumbnailCreator {
    byte[] getThumbnail(File file) throws FileNotFoundException;
}
