/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di.service.file;

import android.content.Context;

import androidx.annotation.NonNull;

import app.notesr.core.security.crypto.AesCryptorFactory;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.FilesUtils;
import app.notesr.data.AppDatabase;
import app.notesr.service.file.FileService;
import app.notesr.service.security.AppSecurityService;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Dagger Hilt module for providing the application's file service.
 * <p>
 * The service uses the application database, security configuration, and file utilities.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class FileServiceModule {

    /**
     * Creates a file service configured with the application's database and encryption.
     *
     * @param context the application context
     * @param appDatabase the application database instance
     * @param appSecurityService the application security service instance
     * @return a {@link FileService} using a cryptor created from the current secrets
     */
    @Provides
    FileService provideFileService(
            @NonNull @ApplicationContext Context context,
            @NonNull AppDatabase appDatabase,
            @NonNull AppSecurityService appSecurityService,
            @NonNull FilesUtils fileUtils) {
        CryptoSecrets cryptoSecrets = appSecurityService.getActualSecrets();

        return new FileService(
                context,
                appDatabase,
                AesCryptorFactory.createAesGcmCryptor(cryptoSecrets),
                fileUtils
        );
    }

}
