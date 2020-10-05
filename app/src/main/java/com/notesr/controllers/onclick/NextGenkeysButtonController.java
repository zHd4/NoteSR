package com.notesr.controllers.onclick;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import com.notesr.controllers.NotesController;
import com.notesr.models.ActivityTools;
import com.notesr.models.Config;
import com.notesr.views.AccessActivity;
import com.notesr.views.MainActivity;
import com.notesr.views.SetupActivity;

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
            String[][] notes = null;

            Bundle extras = setupActivity.getIntent().getExtras();
            boolean isRegeneratingKey = extras != null && extras.getBoolean(SetupActivity.regenerateKey);

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
                        ActivityTools.saveKey(setupActivity.getApplicationContext());
                    } catch (Exception e) {
                        e.printStackTrace();
                        setupActivity.displayRegeneationFailedMessage();
                    }
                }

                setupActivity.startActivity(
                        ActivityTools.getIntent(setupActivity.getApplicationContext(), MainActivity.class)
                );
            } else {
                AccessActivity.operation = AccessActivity.CREATE_PIN;
                setupActivity.startActivity(
                        ActivityTools.getIntent(setupActivity.getApplicationContext(), AccessActivity.class)
                );
            }
        }
    }
}
