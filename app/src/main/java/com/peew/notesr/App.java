package com.peew.notesr;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;

public class App extends Application {
    private static App context;
    private static AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();

        context = this;
        appContainer = new AppContainer();
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

    public boolean isServiceRunning(Class<?> serviceClass) {
        String cleanerName = serviceClass.getName();
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        String foundName = manager.getRunningServices(Integer.MAX_VALUE).stream()
                .map(info -> info.service.getClassName())
                .filter(name -> name.equals(cleanerName))
                .findFirst()
                .orElse(null);

        return foundName != null;
    }
}
