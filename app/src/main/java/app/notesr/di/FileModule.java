/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di;

import android.content.Context;

import androidx.annotation.NonNull;

import app.notesr.core.security.crypto.AesCryptorFactory;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.FilesUtils;
import app.notesr.data.AppDatabase;
import app.notesr.service.file.FileService;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Dagger Hilt module for dependencies used by application note's file operations.
 * <p>
 * Bindings in this module are available from the app-wide component and support
 * file access that depends on application data and security configuration.
 */
@Module
@InstallIn(SingletonComponent.class)
public class FileModule {

    /**
     * Creates a file service configured with the application's database and encryption.
     *
     * @param context the application context
     * @param appDatabase the application database instance
     * @param cryptoManager the manager supplying the current encryption secrets
     * @return a {@link FileService} using a cryptor created from the current secrets
     */
    @Provides
    FileService provideFileService(
            @NonNull @ApplicationContext Context context,
            @NonNull AppDatabase appDatabase,
            @NonNull CryptoManager cryptoManager) {
        CryptoSecrets cryptoSecrets = cryptoManager.getSecrets();

        return new FileService(
                context,
                appDatabase,
                AesCryptorFactory.createAesGcmCryptor(cryptoSecrets),
                new FilesUtils()
        );
    }

}
