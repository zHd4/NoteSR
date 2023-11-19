package com.notesr.controllers.onclick;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import com.notesr.controllers.NotesController;
import com.notesr.controllers.ActivityHelper;
import com.notesr.models.Config;
import com.notesr.models.Note;
import com.notesr.controllers.activities.AccessActivity;
import com.notesr.controllers.activities.MainActivity;
import com.notesr.controllers.activities.SetupActivity;

public class NextGenkeysButtonController implements View.OnClickListener {
    @SuppressWarnings("FieldMayBeFinal")
    private SetupActivity setupActivity;

    private final EditText keyField;

    public NextGenkeysButtonController(final SetupActivity setupActivity, EditText keyField) {
        this.keyField = keyField;
        this.setupActivity = setupActivity;
    }

    @Override
    public void onClick(View v) {
        String importedKey = setupActivity.checkKeyField(keyField.getText().toString());

        if(importedKey != null) {
            Note[] notes = null;

            Bundle extras = setupActivity.getIntent().getExtras();
            boolean isRegeneratingKey = extras != null && extras.getBoolean("regenerateKey");

            if(isRegeneratingKey) {
                try {
                    notes = NotesController.getNotes(setupActivity.getApplicationContext());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if(!importedKey.equals("")) {
                Config.cryptoKey = importedKey;
            } else {
                Config.cryptoKey = setupActivity.tempKey;
            }

            if(isRegeneratingKey) {
                if(notes == null) {
                    setupActivity.displayRegeneationFailedMessage();
                } else {
                    try {
                        NotesController.setNotes(setupActivity.getApplicationContext(), notes);
                        ActivityHelper.saveKey(setupActivity.getApplicationContext());
                    } catch (Exception e) {
                        e.printStackTrace();
                        setupActivity.displayRegeneationFailedMessage();
                    }
                }

                setupActivity.startActivity(
                        ActivityHelper.getIntent(setupActivity.getApplicationContext(), MainActivity.class)
                );
            } else {
                AccessActivity.operation = AccessActivity.CREATE_CODE;
                setupActivity.startActivity(
                        ActivityHelper.getIntent(setupActivity.getApplicationContext(), AccessActivity.class)
                );
            }
        }
    }
}
