package com.peew.notesr.ui.onclick;

import android.content.Intent;
import android.view.View;

import com.peew.notesr.App;
import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.ui.MainActivity;
import com.peew.notesr.ui.manage.NoteOpenActivity;

public class NewNoteOnClick implements View.OnClickListener {
    private static final CryptoManager cryptoManager = CryptoManager.getInstance();
    private final MainActivity activity;

    public NewNoteOnClick(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onClick(View v) {
        if (cryptoManager.getCryptoKeyInstance() != null) {
            Intent noteOpenActivtyIntent = new Intent(App.getContext(), NoteOpenActivity.class);
            noteOpenActivtyIntent.putExtra("mode", NoteOpenActivity.NEW_NOTE_MODE);

            activity.startActivity(noteOpenActivtyIntent);
        }
    }
}
