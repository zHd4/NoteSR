package com.notesr.controllers;

import android.app.Activity;
import android.content.Context;
import com.notesr.views.ChooseFileActivity;

public class Importer {
    public void importFromFile(Context context, Activity activity) {
        ChooseFileActivity.safeCalled = true;
        activity.startActivity(ActivityTools.getIntent(context, ChooseFileActivity.class));
    }
}
