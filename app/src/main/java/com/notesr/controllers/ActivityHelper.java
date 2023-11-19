package com.notesr.controllers;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.notesr.models.Config;
import com.notesr.controllers.activities.AccessActivity;

public class ActivityHelper extends AppCompatActivity {

    public Context context;

    public Context getAppContext() {
        return context;
    }

    public Intent getIntent(Context context, Class<?> activityClass) {
        Intent intent = new Intent(context, activityClass);
        return intent;
    }

    public void showTextMessage(String text, int duration, Context context) {
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    public void checkReady(Context context, AppCompatActivity activity) {
        if(Config.cryptoKey == null || Config.passwordCode == null) {
            activity.startActivity(getIntent(context, AccessActivity.class));
        }
    }
}
