/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.appcompat.app.AppCompatActivity;

public final class ActivityUtils {
    public static void showToastMessage(AppCompatActivity activity, String text, int duration) {
        Toast toast = Toast.makeText(activity, text, duration);
        toast.show();
    }

    public static void copyToClipboard(AppCompatActivity activity, String text) {
        ClipboardManager clipboard =
                (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("", text);

        clipboard.setPrimaryClip(clip);
    }

    public static void disableBackButton(AppCompatActivity activity) {
        OnBackPressedDispatcher dispatcher = activity.getOnBackPressedDispatcher();
        dispatcher.addCallback(activity, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                activity.finishAffinity();
            }
        });
    }
}
