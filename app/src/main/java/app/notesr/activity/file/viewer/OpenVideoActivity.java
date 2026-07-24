/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.file.viewer;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.WindowManager;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.PlayerView;

import app.notesr.R;
import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesCryptorFactory;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.crypto.EncryptedMediaDataSourceFactory;
import app.notesr.core.util.FilesUtils;
import app.notesr.data.DatabaseProvider;
import app.notesr.service.file.FileService;
import app.notesr.service.security.AppSecurityService;

import java.io.File;
import java.util.stream.Collectors;

public final class OpenVideoActivity extends FileViewerActivityBase {

    private static final int CACHE_VIDEO_BLOCKS = 4;
    private static final int BLOCK_METADATA_LENGTH = AesGcmCryptor.IV_SIZE
            + AesGcmCryptor.TAG_LENGTH;

    private FileService fileService;
    private PlayerView videoView;
    private ExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_open_video);
        applyInsets(findViewById(R.id.main));

        saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);

        var context = getApplicationContext();
        var db = DatabaseProvider.getInstance(context);

        AesCryptor cryptor = AesCryptorFactory.createAesGcmCryptor(
                new AppSecurityService(context).getActualSecrets());

        fileService = new FileService(context, db, cryptor, new FilesUtils());
        videoView = findViewById(R.id.video_view);

        configurePlayer(cryptor);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void configurePlayer(AesCryptor cryptor) {
        player = new ExoPlayer.Builder(getApplicationContext()).build();
        videoView.setPlayer(player);

        var mediaItem = new MediaItem.Builder()
                .setUri(Uri.EMPTY)
                .build();

        player.setMediaItem(mediaItem);

        var blocksDir = new File(getFilesDir(), FileService.BLOBS_DIR_NAME);

        newSingleThreadExecutor().execute(() -> {
            var blockFiles = fileService.getFileBlobInfoIds(fileInfo.getId()).stream()
                    .map(blockId -> new File(blocksDir, blockId))
                    .collect(Collectors.toList());

            var dataSourceFactory = new EncryptedMediaDataSourceFactory(cryptor,
                    blockFiles, BLOCK_METADATA_LENGTH, CACHE_VIDEO_BLOCKS);

            var mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(mediaItem);

            runOnUiThread(() -> {
                player.setMediaSource(mediaSource);
                player.prepare();
            });
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        player.stop();
    }
}
