/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di;

import android.content.Context;

import androidx.annotation.NonNull;

import javax.inject.Singleton;

import app.notesr.activity.file.helper.FileIOHelper;
import app.notesr.core.util.FilesUtils;
import app.notesr.core.util.TransactionalFilesUtil;
import app.notesr.core.util.VersionFetcher;
import app.notesr.service.file.FileService;
import app.notesr.util.VersionFetcherImpl;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Dagger Hilt module for application-wide utility dependencies.
 * <p>
 * Bindings in this module make shared utility dependencies available from the
 * app-wide component.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class UtilityModule {

    /**
     * Provides the application-scoped implementation for retrieving app version information.
     *
     * @return the {@link VersionFetcher} implementation
     */
    @Provides
    @Singleton
    VersionFetcher provideVersionFetcher() {
        return new VersionFetcherImpl();
    }

    /**
     * Provides the shared file utility instance.
     *
     * @return the application-wide {@link FilesUtils} instance
     */
    @Provides
    @Singleton
    FilesUtils provideFilesUtils() {
        return new FilesUtils();
    }

    /**
     * Provides a utility for transactional file operations.
     *
     * @param context the application context
     * @param filesUtils the shared file utility instance
     * @return a {@link TransactionalFilesUtil} using the supplied context and utilities
     */
    @Provides
    TransactionalFilesUtil provideTransactionalFilesUtil(
            @ApplicationContext Context context, @NonNull FilesUtils filesUtils) {
        return new TransactionalFilesUtil(context, filesUtils);
    }

    /**
     * Provides the file I/O helper used by file activities file operations.
     *
     * @param context the file utility instance
     * @param fileService the service used for file operations
     * @return a {@link FileIOHelper} backed by the supplied utilities and service
     */
    @Provides
    FileIOHelper provideFileIOHelper(
            @NonNull FilesUtils context, @NonNull FileService fileService) {
        return new FileIOHelper(context, fileService);
    }
}
