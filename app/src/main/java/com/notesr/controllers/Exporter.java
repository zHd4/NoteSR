package com.notesr.controllers;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Environment;
import android.util.Base64;
import android.widget.Toast;

import com.notesr.controllers.crypto.CryptoController;
import com.notesr.controllers.db.DatabaseController;
import com.notesr.controllers.managers.HashManager;
import com.notesr.models.Config;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/** @noinspection resource*/
public class Exporter extends ActivityHelper {
    public void exportToClipboard(Context context, ClipboardManager clipboardManager) {
        try {
            DatabaseController db = new DatabaseController(context);

            String notesData = Base64.encodeToString(CryptoController.encrypt(
                    db.exportToJsonString(context).getBytes(),
                    HashManager.toSha256String(Config.cryptoKey),
                    Base64.decode(Config.cryptoKey, Base64.DEFAULT)
            ), Base64.DEFAULT);

            ClipData clip = ClipData.newPlainText("", notesData);

            clipboardManager.setPrimaryClip(clip);
            showTextMessage("Copied!", Toast.LENGTH_SHORT, context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportToFile(Context context) {
        try {
            DatabaseController db = new DatabaseController(context);

            @SuppressLint("SimpleDateFormat")
            String datetime = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(
                    Calendar.getInstance().getTime()
            );

            File path = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    String.format("notesr_export_" + datetime + ".nsrbak")
            );

            String notesData = Base64.encodeToString(CryptoController.encrypt(
                    db.exportToJsonString(context).getBytes(),
                    HashManager.toSha256String(Config.cryptoKey),
                    Base64.decode(Config.cryptoKey, Base64.DEFAULT)
            ), Base64.DEFAULT);

            if (StorageController.externalWriteFile(path, notesData)) {
                showTextMessage("Saved to " + path.getAbsolutePath(), Toast.LENGTH_SHORT, context);
            } else {
                showTextMessage(
                        "Please allow storage access and try again",
                        Toast.LENGTH_SHORT,
                        context
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
