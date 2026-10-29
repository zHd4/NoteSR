/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di;

import javax.inject.Singleton;

import app.notesr.core.util.VersionFetcher;
import app.notesr.util.VersionFetcherImpl;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Dagger Hilt module for application-wide utility dependencies.
 * <p>
 * Bindings in this module make shared utility dependencies available from the
 * app-wide component.
 */
@Module
@InstallIn(SingletonComponent.class)
public class UtilityModule {

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
}
