package com.peew.notesr;

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
}
