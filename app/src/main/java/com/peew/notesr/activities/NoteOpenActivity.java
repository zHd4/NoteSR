package com.peew.notesr.activities;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import android.os.Bundle;
import android.widget.EditText;
import androidx.appcompat.app.ActionBar;

import com.peew.notesr.R;

public class NoteOpenActivity extends ExtendedAppCompatActivity {
    public static final Integer NEW_NOTE_MODE = 0;
    public static final Integer EDIT_NOTE_MODE = 1;

    private int mode;

    /** @noinspection DataFlowIssue*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_open);

        mode = getIntent().getIntExtra("mode", NEW_NOTE_MODE);

        EditText nameField = findViewById(R.id.note_name_field);
        EditText textField = findViewById(R.id.note_text_field);

        nameField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);
        textField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (mode == EDIT_NOTE_MODE) {
            actionBar.setTitle(getResources().getString(R.string.edit_note));
        }
    }
}