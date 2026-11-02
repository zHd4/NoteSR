/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di.util;

import javax.inject.Singleton;

import app.notesr.core.util.VersionFetcher;
import app.notesr.util.VersionFetcherImpl;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Provides the singleton implementation of app version lookup.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class VersionFetcherModule {

    @Provides
    @Singleton
    VersionFetcher provideVersionFetcher() {
        return new VersionFetcherImpl();
    }
}
