/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.note;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import android.app.Dialog;
import android.content.Intent;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.annotation.NonNull;

import java.time.LocalDateTime;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.DialogFactory;
import app.notesr.data.model.Note;
import app.notesr.service.note.NoteService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SaveNoteOnClick implements MenuItem.OnMenuItemClickListener {

    private final ActivityBase activity;
    private final Note note;
    private final NoteService noteService;
    private final DialogFactory dialogFactory;
    private final EditText nameField;
    private final EditText textField;

    @Override
    public boolean onMenuItemClick(@NonNull MenuItem item) {
        String name = nameField.getText().toString();
        String text = textField.getText().toString();

        if (!name.isBlank() && !text.isBlank()) {
            note.setName(name);
            note.setText(text);
            note.setUpdatedAt(LocalDateTime.now());

            Dialog progressDialog = dialogFactory
                    .getThemedProgressDialog(R.layout.progress_dialog_loading);

            newSingleThreadExecutor().execute(() -> {
                activity.runOnUiThread(progressDialog::show);
                noteService.save(note);

                activity.runOnUiThread(() -> {
                    progressDialog.dismiss();
                    activity.startActivity(new Intent(activity, NotesListActivity.class));
                });
            });
        }

        return true;
    }
}
