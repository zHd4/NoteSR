package com.notesr.models;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Environment;
import android.util.Base64;
import android.widget.Toast;
import com.notesr.controllers.Crypto;
import com.notesr.controllers.Database;
import com.notesr.controllers.Storage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Exporter {
    private final Activity activity;

    public Exporter(Activity activity) {
        this.activity = activity;
    }

    public void exportToClipboard(Context context, ClipboardManager clipboardManager) {
        try {
            Database db = new Database(context);

            String notesData = Crypto.encrypt(
                    db.exportToJsonString(context),
                    ActivityTools.sha256(Config.cryptoKey),
                    Base64.decode(Config.cryptoKey, Base64.DEFAULT)
            );

            ActivityTools.clipboard = clipboardManager;

            ClipData clip = ClipData.newPlainText("", notesData);

            ActivityTools.clipboard.setPrimaryClip(clip);

            ActivityTools.showTextMessage("Copied!", Toast.LENGTH_SHORT, context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportToFile(Context context) {
        try {
            Database db = new Database(context);

            @SuppressLint("SimpleDateFormat")
            String datetime = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(
                    Calendar.getInstance().getTime()
            );

            File path = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    String.format("notesr_export_" + datetime + ".nsrbak")
            );

            String notesData = Crypto.encrypt(
                    db.exportToJsonString(context),
                    ActivityTools.sha256(Config.cryptoKey),
                    Base64.decode(Config.cryptoKey, Base64.DEFAULT)
            );

            if (Storage.externalWriteFile(path, notesData)) {
                ActivityTools.showTextMessage("Saved to " + path.getAbsolutePath(), Toast.LENGTH_SHORT, context);
            } else {
                ActivityTools.showTextMessage(
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
