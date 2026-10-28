/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di;

import android.content.Context;

import androidx.annotation.NonNull;

import app.notesr.service.AndroidServiceBootstrapper;
import app.notesr.service.AndroidServiceRegistry;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Provides Android Service Framework dependencies to the app-wide Hilt component.
 */
@Module
@InstallIn(SingletonComponent.class)
public class AndroidServiceModule {

    /**
     * Provides the {@link AndroidServiceRegistry} instance for tracking running services.
     *
     * @param context the application context
     * @return the singleton AndroidServiceRegistry instance
     */
    @Provides
    AndroidServiceRegistry provideAndroidServiceRegistry(
            @NonNull @ApplicationContext Context context) {
        return AndroidServiceRegistry.getInstance(context);
    }

    /**
     * Provides the {@link AndroidServiceBootstrapper} instance for managing auto-start services.
     *
     * @param registry the AndroidServiceRegistry instance
     * @return an AndroidServiceBootstrapper backed by the supplied registry
     */
    @Provides
    AndroidServiceBootstrapper provideAndroidServiceBootstrapper(
            @NonNull AndroidServiceRegistry registry) {
        return new AndroidServiceBootstrapper(registry);
    }
}
