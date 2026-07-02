/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.note.editor;

import android.content.DialogInterface;
import android.content.Intent;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.DialogFactory;
import app.notesr.activity.file.FilesListActivity;
import app.notesr.data.model.Note;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class OpenFilesListAction {

    private final ActivityBase activity;
    private final DialogFactory dialogFactory;
    private final SaveNoteAction saveNoteAction;
    private final Note note;

    void execute(boolean isNoteFieldsModified) {
        if (isNoteFieldsModified && saveNoteAction.isFormCorrect()) {
            DialogInterface.OnClickListener buttonHandler = (dialog, result) -> {
                if (result == DialogInterface.BUTTON_POSITIVE) {
                    saveNoteAction.execute();
                }

                startFilesListActivity(true);
                activity.finish();
            };

            dialogFactory.getThemedAlertDialogBuilder(R.layout.dialog_unsaved_note_changes)
                    .setTitle(R.string.warning)
                    .setPositiveButton(R.string.save, buttonHandler)
                    .setNeutralButton(R.string.dont_save, buttonHandler)
                    .setNegativeButton(R.string.cancel, null)
                    .create()
                    .show();
        } else {
            startFilesListActivity(false);
        }
    }

    private void startFilesListActivity(boolean isNoteFieldsModified) {
        Intent intent = new Intent(activity.getApplicationContext(), FilesListActivity.class)
                .putExtra(FilesListActivity.EXTRA_NOTE_ID, note.getId())
                .putExtra(FilesListActivity.EXTRA_NOTE_FIELDS_WERE_MODIFIED, isNoteFieldsModified);

        activity.startActivity(intent);
    }
}
