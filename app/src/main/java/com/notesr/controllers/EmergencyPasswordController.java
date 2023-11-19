package com.notesr.controllers;

import android.content.Context;

import com.notesr.controllers.managers.HashManager;
import com.notesr.models.Config;

public class EmergencyPasswordController {
    private final String password;

    private final Context context;

    public EmergencyPasswordController(Context context, String password) {
        this.password = password;
        this.context = context;
    }

    public void setPin() {
        String pinHash = HashManager.toSha256String(password);
        StorageController.writeFile(this.context, Config.secretPinFileName, pinHash);
    }

    public boolean checkPin() {
        String haystackHash = HashManager.toSha256String(password);
        String needleHash = StorageController.readFile(context, Config.secretPinFileName);

        return haystackHash.equals(needleHash);
    }
}
