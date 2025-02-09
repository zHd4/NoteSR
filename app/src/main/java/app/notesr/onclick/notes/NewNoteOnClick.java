package app.notesr.onclick.notes;

import android.content.Intent;
import android.view.View;

import app.notesr.App;
import app.notesr.activity.notes.NoteListActivity;
import app.notesr.activity.notes.OpenNoteActivity;

public class NewNoteOnClick implements View.OnClickListener {
    private final NoteListActivity activity;

    public NewNoteOnClick(NoteListActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onClick(View v) {
        if (App.getAppContainer().getCryptoManager().getCryptoKeyInstance() != null) {
            activity.startActivity(new Intent(App.getContext(), OpenNoteActivity.class));
        }
    }
}
