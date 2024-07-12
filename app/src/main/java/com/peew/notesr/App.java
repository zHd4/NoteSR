package com.peew.notesr;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import com.peew.notesr.service.CacheCleanService;

public class App extends Application {
    private static App context;
    private static AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();

        context = this;
        appContainer = new AppContainer();

        startServices();
    }

    public static Context getContext() {
        return context;
    }

    public static AppContainer getAppContainer() {
        return appContainer;
    }

    public static boolean onAndroid() {
        return context != null;
    }

    private void startServices() {
        startForegroundService(new Intent(this, CacheCleanService.class));
    }
}
