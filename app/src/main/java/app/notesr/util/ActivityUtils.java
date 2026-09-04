/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;

import app.notesr.activity.ActivityBase;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ActivityUtils {
    @NonNull
    private final ActivityBase activity;

    public void showToastMessage(String text, int duration) {
        Toast toast = Toast.makeText(activity, text, duration);
        toast.show();
    }

    public void copyToClipboard(String text) {
        ClipboardManager clipboard =
                (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("", text);

        clipboard.setPrimaryClip(clip);
    }

    public void disableBackButton() {
        OnBackPressedDispatcher dispatcher = activity.getOnBackPressedDispatcher();
        dispatcher.addCallback(activity, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                activity.finishAffinity();
            }
        });
    }
}
