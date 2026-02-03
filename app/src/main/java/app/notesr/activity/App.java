/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import lombok.Getter;

public final class App extends Application implements Application.ActivityLifecycleCallbacks {
    @Getter
    private static App context;

    private WeakReference<Activity> currentActivityRef = new WeakReference<>(null);

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;

        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivityRef = new WeakReference<>(activity);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        Activity current = currentActivityRef.get();

        if (current == activity) {
            currentActivityRef.clear();
        }
    }

    public boolean isServiceRunning(Class<?> serviceClass) {
        String serviceName = serviceClass.getName();
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        String foundName = manager.getRunningServices(Integer.MAX_VALUE).stream()
                .map(info -> info.service.getClassName())
                .filter(name -> name.equals(serviceName))
                .findFirst()
                .orElse(null);

        return foundName != null;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) { }

    @Override
    public void onActivityStarted(@NonNull Activity activity) { }

    @Override
    public void onActivityStopped(@NonNull Activity activity) { }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) { }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) { }
}
