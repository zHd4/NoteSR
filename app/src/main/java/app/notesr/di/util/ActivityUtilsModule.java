/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di.util;

import android.content.Context;

import app.notesr.activity.ActivityBase;
import app.notesr.util.ActivityUtils;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.qualifiers.ActivityContext;
import dagger.hilt.android.scopes.ActivityScoped;

/**
 * Provides activity-scoped {@link ActivityUtils} instances for the current activity.
 */
@Module
@InstallIn(ActivityComponent.class)
public final class ActivityUtilsModule {

    /**
     * Provides the activity utilities for the current activity.
     *
     * @param context the current activity context
     * @return the {@link ActivityUtils} instance for the activity
     */
    @Provides
    @ActivityScoped
    ActivityUtils provideActivityUtils(@ActivityContext Context context) {
        return new ActivityUtils((ActivityBase) context);
    }
}
