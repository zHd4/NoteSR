/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di;

import android.content.Context;

import androidx.annotation.NonNull;

import javax.inject.Singleton;

import app.notesr.data.AppDatabase;
import app.notesr.data.DatabaseProvider;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Dagger Hilt module for providing data layer dependencies.
 * <p>
 * This module provides the {@link AppDatabase} instance for data operations,
 * including database access and data persistence.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class DataModule {

    /**
     * Provides the {@link AppDatabase} instance for database operations.
     *
     * @param context the application context
     * @return the singleton AppDatabase instance
     */
    @Provides
    AppDatabase provideAppDatabase(
            @NonNull @ApplicationContext Context context) {
        return DatabaseProvider.getInstance(context);
    }
}
