package app.notesr.activity.files.viewer;

import java.io.File;
import java.io.FileNotFoundException;

import app.notesr.App;
import app.notesr.service.FilesService;
import app.notesr.utils.thumbnail.ImageThumbnailCreator;
import app.notesr.utils.thumbnail.ThumbnailCreator;
import app.notesr.utils.thumbnail.VideoThumbnailCreator;

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
            creator = new VideoThumbnailCreator(getApplicationContext());
        } else {
            throw new RuntimeException("Unexpected media file type: " + type);
        }

        try {
            byte[] thumbnail = creator.getThumbnail(mediaFile);
            FilesService filesService = App.getAppContainer().getFilesService();

            fileInfo.setThumbnail(thumbnail);
            filesService.saveInfo(fileInfo);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
