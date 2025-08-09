package app.notesr.file.activity.viewer;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import java.io.File;
import java.io.FileNotFoundException;

import app.notesr.db.AppDatabase;
import app.notesr.db.DatabaseProvider;
import app.notesr.file.service.FileService;
import app.notesr.util.thumbnail.ImageThumbnailCreator;
import app.notesr.util.thumbnail.ThumbnailCreator;
import app.notesr.util.thumbnail.VideoThumbnailCreator;

public class MediaFileViewerActivityBase extends FileViewerActivityBase {

    protected boolean isThumbnailSet() {
        return fileInfo.getThumbnail() != null;
    }

    protected void setThumbnail(File mediaFile) {
        String type = fileInfo.getType().split("/")[0];
        ThumbnailCreator creator;

        if (type.equals("image")) {
            creator = new ImageThumbnailCreator();
        } else if (type.equals("video")) {
            creator = new VideoThumbnailCreator();
        } else {
            throw new RuntimeException("Unexpected media file type: " + type);
        }

        try {
            byte[] thumbnail = creator.getThumbnail(mediaFile);

            AppDatabase db = DatabaseProvider.getInstance(getApplicationContext());
            FileService fileService = new FileService(db);

            fileInfo.setThumbnail(thumbnail);
            newSingleThreadExecutor().execute(() -> fileService.saveInfo(fileInfo));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
