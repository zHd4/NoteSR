/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.file.viewer;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;

import app.notesr.core.util.FilesUtils;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.IOException;

import app.notesr.data.AppDatabase;
import app.notesr.data.DatabaseProvider;
import app.notesr.service.file.FileService;
import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.thumbnail.ImageThumbnailCreator;
import app.notesr.core.util.thumbnail.ThumbnailCreator;
import app.notesr.core.util.thumbnail.VideoThumbnailCreator;

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

            CryptoSecrets secrets = CryptoManagerProvider.getInstance(context).getSecrets();
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
