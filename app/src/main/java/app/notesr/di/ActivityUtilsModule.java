/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di;

import android.content.Context;

import app.notesr.activity.ActivityBase;
import app.notesr.util.ActivityUtils;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.qualifiers.ActivityContext;
import dagger.hilt.android.scopes.ActivityScoped;

@Module
@InstallIn(ActivityComponent.class)
public final class ActivityUtilsModule {

    @Provides
    @ActivityScoped
    ActivityUtils provideActivityUtils(@ActivityContext Context context) {
        return new ActivityUtils((ActivityBase) context);
    }
}
