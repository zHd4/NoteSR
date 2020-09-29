package com.notesr.controllers;

import android.content.Context;
import com.notesr.models.ActivityTools;
import com.notesr.models.Config;

public class SecretPinController {
    private final int pin;

    @SuppressWarnings("FieldMayBeFinal")
    private Context context;

    public SecretPinController(Context context, int pin) {
        this.pin = pin;
        this.context = context;
    }

    public void setPin() {
        String pinHash = ActivityTools.sha256(String.valueOf(pin));
        StorageController.writeFile(this.context, Config.secretPinFileNameName, pinHash);
    }

    public boolean checkPin() {
        String haystackHash = ActivityTools.sha256(String.valueOf(pin));
        String needleHash = StorageController.readFile(context, Config.secretPinFileNameName);

        return haystackHash.equals(needleHash);
    }
}
