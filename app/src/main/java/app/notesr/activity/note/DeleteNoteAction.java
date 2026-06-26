/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.note;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;

import java.io.IOException;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.DialogFactory;
import app.notesr.data.model.Note;
import app.notesr.service.file.FileService;
import app.notesr.service.note.NoteService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class DeleteNoteAction {

    private final ActivityBase activity;
    private final Note note;
    private final NoteService noteService;
    private final FileService fileService;
    private final DialogFactory dialogFactory;

    void execute() {
        DialogInterface.OnClickListener buttonHandler = deleteNoteDialogOnClick();
        dialogFactory.getThemedAlertDialogBuilder(R.layout.dialog_action_cannot_be_undo)
                .setTitle(R.string.warning)
                .setPositiveButton(R.string.delete_caps, buttonHandler)
                .setNegativeButton(R.string.no, buttonHandler)
                .create()
                .show();
    }

    private DialogInterface.OnClickListener deleteNoteDialogOnClick() {
        return (dialog, result) -> {
            if (result == DialogInterface.BUTTON_POSITIVE) {
                Dialog progressDialog = dialogFactory
                        .getThemedProgressDialog(R.layout.progress_dialog_deleting);

                newSingleThreadExecutor().execute(() -> {
                    activity.runOnUiThread(progressDialog::show);

                    try {
                        noteService.delete(note.getId(), fileService);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    activity.runOnUiThread(() -> {
                        progressDialog.dismiss();
                        activity.startActivity(new Intent(activity.getApplicationContext(),
                                NotesListActivity.class));
                    });
                });
            }
        };
    }
}
