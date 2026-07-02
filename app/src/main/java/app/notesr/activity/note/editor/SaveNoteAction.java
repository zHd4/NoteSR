/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.note.editor;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import android.app.Dialog;
import android.content.Intent;
import android.widget.EditText;

import java.time.LocalDateTime;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.DialogFactory;
import app.notesr.activity.note.list.NotesListActivity;
import app.notesr.data.model.Note;
import app.notesr.service.note.NoteService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class SaveNoteAction {

    private final ActivityBase activity;
    private final Note note;
    private final NoteService noteService;
    private final DialogFactory dialogFactory;
    private final EditText nameField;
    private final EditText textField;

    boolean isFormCorrect() {
        return !nameField.getText().toString().isBlank()
                && !textField.getText().toString().isBlank();
    }

    void execute() {
        if (isFormCorrect()) {
            note.setName(nameField.getText().toString());
            note.setText(textField.getText().toString());
            note.setUpdatedAt(LocalDateTime.now());

            Dialog progressDialog = dialogFactory
                    .getThemedProgressDialog(R.layout.progress_dialog_loading);

            newSingleThreadExecutor().execute(() -> {
                activity.runOnUiThread(progressDialog::show);
                noteService.save(note);
            });
        }
    }
}
