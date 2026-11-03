/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di.util;

import androidx.annotation.NonNull;

import app.notesr.activity.file.helper.FileIOHelper;
import app.notesr.core.util.FilesUtils;
import app.notesr.service.file.FileService;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Provides the helper used by activities for file I/O operations.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class FileIOHelperModule {

    @Provides
    FileIOHelper provideFileIOHelper(
            @NonNull FilesUtils filesUtils, @NonNull FileService fileService) {
        return new FileIOHelper(filesUtils, fileService);
    }
}
