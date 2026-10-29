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
 * Dagger Hilt module for integrating Android Services Framework with the application.
 * <p>
 * Bindings in this module make the dependencies needed by Android service
 * management available from the app-wide component.
 */
@Module
@InstallIn(SingletonComponent.class)
public class AndroidServiceModule {

    /**
     * Provides the registry used to track running Android services.
     *
     * @param context the application context
     * @return the shared {@link AndroidServiceRegistry} for the application
     */
    @Provides
    AndroidServiceRegistry provideAndroidServiceRegistry(
            @NonNull @ApplicationContext Context context) {
        return AndroidServiceRegistry.getInstance(context);
    }

    /**
     * Creates a bootstrapper for managing Android services that start automatically.
     *
     * @param registry the AndroidServiceRegistry instance
     * @return an {@link AndroidServiceBootstrapper} backed by the supplied registry
     */
    @Provides
    AndroidServiceBootstrapper provideAndroidServiceBootstrapper(
            @NonNull AndroidServiceRegistry registry) {
        return new AndroidServiceBootstrapper(registry);
    }
}
