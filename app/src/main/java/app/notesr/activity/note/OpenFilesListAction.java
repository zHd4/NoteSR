/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.note;

import android.content.Intent;

import app.notesr.activity.ActivityBase;
import app.notesr.activity.file.FilesListActivity;
import app.notesr.data.model.Note;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class OpenFilesListAction {

    private final ActivityBase activity;
    private final Note note;

    void execute() {
        Intent intent = new Intent(activity.getApplicationContext(), FilesListActivity.class);

        intent.putExtra(FilesListActivity.EXTRA_NOTE_ID, note.getId());
        activity.startActivity(intent);
    }
}
