package com.peew.notesr.activities;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import android.os.Bundle;
import android.widget.EditText;

import com.peew.notesr.R;

public class NoteOpenActivity extends ExtendedAppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_open);

        EditText nameField = findViewById(R.id.note_name_field);
        EditText textField = findViewById(R.id.note_text_field);

        nameField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);
        textField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);
    }
}