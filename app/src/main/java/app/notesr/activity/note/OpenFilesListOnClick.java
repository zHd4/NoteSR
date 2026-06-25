/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.note;

import android.content.Intent;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;

import app.notesr.activity.ActivityBase;
import app.notesr.activity.file.FilesListActivity;
import app.notesr.data.model.Note;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class OpenFilesListOnClick implements MenuItem.OnMenuItemClickListener, View.OnClickListener {

    private final ActivityBase activity;
    private final Note note;

    @Override
    public boolean onMenuItemClick(@NonNull MenuItem item) {
        onClickAction();
        return true;
    }

    @Override
    public void onClick(View v) {
        onClickAction();
    }

    private void onClickAction() {
        Intent intent = new Intent(activity.getApplicationContext(), FilesListActivity.class);

        intent.putExtra(FilesListActivity.EXTRA_NOTE_ID, note.getId());
        activity.startActivity(intent);
    }
}
