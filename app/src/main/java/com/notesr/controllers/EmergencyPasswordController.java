package com.notesr.controllers;

import android.content.Context;
import com.notesr.models.Config;

public class EmergencyPasswordController {
    private final String password;

    @SuppressWarnings("FieldMayBeFinal")
    private Context context;

    public EmergencyPasswordController(Context context, String password) {
        this.password = password;
        this.context = context;
    }

    public void setPin() {
        String pinHash = ActivityTools.sha256(password);
        StorageController.writeFile(this.context, Config.secretPinFileNameName, pinHash);
    }

    public boolean checkPin() {
        String haystackHash = ActivityTools.sha256(password);
        String needleHash = StorageController.readFile(context, Config.secretPinFileNameName);

        return haystackHash.equals(needleHash);
    }
}
