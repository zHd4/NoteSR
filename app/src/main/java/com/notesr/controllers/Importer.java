package com.notesr.controllers;

import android.app.Activity;
import android.content.Context;
import com.notesr.controllers.activities.ChooseFileActivity;

public class Importer extends ActivityHelper {
    public void importFromFile(Context context, Activity activity) {
        ChooseFileActivity.safeCalled = true;
        activity.startActivity(getIntent(context, ChooseFileActivity.class));
    }
}
