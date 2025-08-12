package app.notesr.util;

import java.io.File;
import java.io.IOException;

public interface WiperAdapter {
    void wipeFile(File file) throws IOException;
}
