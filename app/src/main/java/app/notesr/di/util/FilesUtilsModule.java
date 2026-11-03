/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di.util;

import javax.inject.Singleton;

import app.notesr.core.util.FilesUtils;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Provides the singleton file utility shared across the application.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class FilesUtilsModule {

    @Provides
    @Singleton
    FilesUtils provideFilesUtils() {
        return new FilesUtils();
    }
}
