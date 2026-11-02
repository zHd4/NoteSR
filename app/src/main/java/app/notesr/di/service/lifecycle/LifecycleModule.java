/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di.service.lifecycle;

import app.notesr.service.lifecycle.AppCloseAndroidServiceStarter;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Dagger Hilt module for providing the Android service starter used on app close.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class LifecycleModule {

    /**
     * Creates a starter for the Android service invoked when the app closes.
     *
     * @return an {@link AppCloseAndroidServiceStarter}
     */
    @Provides
    AppCloseAndroidServiceStarter provideAppCloseAndroidServiceStarter() {
        return new AppCloseAndroidServiceStarter();
    }
}
