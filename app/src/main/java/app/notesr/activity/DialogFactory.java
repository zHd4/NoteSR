/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity;

import android.app.Activity;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AlertDialog;

import app.notesr.R;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class DialogFactory {
    private final Activity activity;

    public AlertDialog.Builder themedAlertDialogBuilder(@LayoutRes int layoutRes) {
        return new AlertDialog.Builder(activity, R.style.AlertDialogTheme)
                .setView(layoutRes);
    }

    public AlertDialog getThemedProgressDialog(@LayoutRes int layoutRes) {
        return themedAlertDialogBuilder(layoutRes)
                .setCancelable(false)
                .create();
    }
}
