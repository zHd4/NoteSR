/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di;

import android.app.Activity;
import android.content.Context;

import androidx.media3.exoplayer.ExoPlayer;

import app.notesr.activity.ActivityBase;
import app.notesr.activity.DialogFactory;
import app.notesr.util.ActivityUtils;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.qualifiers.ActivityContext;
import dagger.hilt.android.scopes.ActivityScoped;

/**
 * Dagger Hilt module for dependencies associated with an activity.
 * <p>
 * This module provides activity-scoped utilities and UI support dependencies.
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

    /**
     * Provides a dialog factory for the current activity.
     *
     * @param context the current activity context
     * @return the {@link DialogFactory} instance for the activity
     */
    @Provides
    @ActivityScoped
    DialogFactory provideDialogFactory(@ActivityContext Context context) {
        return new DialogFactory((Activity) context);
    }

    /**
     * Provides the media player for the current activity.
     *
     * @param context the current activity context
     * @return the activity-scoped {@link ExoPlayer}
     */
    @Provides
    @ActivityScoped
    ExoPlayer provideExoPlayer(@ActivityContext Context context) {
        return new ExoPlayer.Builder(context).build();
    }
}
