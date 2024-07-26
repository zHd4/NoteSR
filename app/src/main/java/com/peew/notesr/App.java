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

    public static App getContext() {
        return context;
    }

    public static AppContainer getAppContainer() {
        return appContainer;
    }

    public static boolean onAndroid() {
        return context != null;
    }

    public boolean serviceRunning(Class<?> serviceClass) {
        String serviceName = serviceClass.getName();
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        String foundName = manager.getRunningServices(Integer.MAX_VALUE).stream()
                .map(info -> info.service.getClassName())
                .filter(name -> name.equals(serviceName))
                .findFirst()
                .orElse(null);

        return foundName != null;
    }
}
