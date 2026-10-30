/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di;

import android.content.Context;

import androidx.annotation.NonNull;

import javax.inject.Singleton;

import app.notesr.service.migration.DataVersionManager;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Dagger Hilt module for providing migration-related services.
 * <p>
 * This module provides dependencies for application data migration operations,
 * including schema version tracking and database upgrades.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class MigrationModule {
    /**
     * Provides the {@link DataVersionManager} instance for tracking data schema version.
     *
     * @param context the application context
     * @return the singleton DataVersionManager instance
     */
    @Provides
    DataVersionManager provideDataVersionManager(
            @NonNull @ApplicationContext Context context) {
        return new DataVersionManager(context);
    }
}
