/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di.util;

import android.content.Context;

import androidx.annotation.NonNull;

import app.notesr.core.util.FilesUtils;
import app.notesr.core.util.TransactionalFilesUtil;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Provides the application-scoped dependency for transactional file operations.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class TransactionalFilesUtilModule {

    @Provides
    TransactionalFilesUtil provideTransactionalFilesUtil(
            @ApplicationContext Context context, @NonNull FilesUtils filesUtils) {
        return new TransactionalFilesUtil(context, filesUtils);
    }
}
