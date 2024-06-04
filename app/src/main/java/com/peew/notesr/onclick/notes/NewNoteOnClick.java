package com.peew.notesr.onclick.notes;

import android.content.Intent;
import android.view.View;

import com.peew.notesr.App;
import com.peew.notesr.activity.MainActivity;
import com.peew.notesr.activity.NoteOpenActivity;

public class NewNoteOnClick implements View.OnClickListener {
    private final MainActivity activity;

    public NewNoteOnClick(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onClick(View v) {
        if (App.getAppContainer().getCryptoManager().getCryptoKeyInstance() != null) {
            activity.startActivity(new Intent(App.getContext(), NoteOpenActivity.class));
        }
    }
}
