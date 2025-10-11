package app.notesr.file.activity.viewer;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import static app.notesr.util.KeyUtils.getSecretKeyFromSecrets;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.IOException;

import app.notesr.db.AppDatabase;
import app.notesr.db.DatabaseProvider;
import app.notesr.file.service.FileService;
import app.notesr.security.crypto.AesCryptor;
import app.notesr.security.crypto.AesGcmCryptor;
import app.notesr.security.crypto.CryptoManagerProvider;
import app.notesr.security.dto.CryptoSecrets;
import app.notesr.util.FilesUtils;
import app.notesr.util.thumbnail.ImageThumbnailCreator;
import app.notesr.util.thumbnail.ThumbnailCreator;
import app.notesr.util.thumbnail.VideoThumbnailCreator;

public class MediaFileViewerActivityBase extends FileViewerActivityBase {

    protected final boolean isThumbnailSet() {
        return fileInfo.getThumbnail() != null;
    }

    protected final void setThumbnail(File mediaFile) {
        String type = fileInfo.getType().split("/")[0];
        ThumbnailCreator creator;

        if (type.equals("image")) {
            creator = new ImageThumbnailCreator(getApplicationContext(), new FilesUtils());
        } else if (type.equals("video")) {
            creator = new VideoThumbnailCreator(getApplicationContext());
        } else {
            throw new RuntimeException("Unexpected media file type: " + type);
        }

        try {
            byte[] thumbnail = creator.getThumbnail(Uri.fromFile(mediaFile));

            Context context = getApplicationContext();
            AppDatabase db = DatabaseProvider.getInstance(context);

            CryptoSecrets secrets = CryptoManagerProvider.getInstance().getSecrets();
            AesCryptor cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(secrets));

            FileService fileService = new FileService(context, db, cryptor, new FilesUtils());

            fileInfo.setThumbnail(thumbnail);

            newSingleThreadExecutor().execute(() ->
                    fileService.setThumbnail(fileInfo.getId(), thumbnail));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
