/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.file;

import android.content.Intent;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class AddFileOnClick implements View.OnClickListener {

    private final ActivityBase activity;
    private final ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    public void onClick(View v) {
        Intent intent = new Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT);

        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        filePickerLauncher.launch(Intent.createChooser(intent,
                activity.getString(R.string.choose_files)));
    }
}
