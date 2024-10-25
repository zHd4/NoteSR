package app.notesr.onclick.notes;

import android.content.Intent;
import android.view.View;

import app.notesr.App;
import app.notesr.activity.notes.NotesListActivity;
import app.notesr.activity.notes.OpenNoteActivity;

public class NewNoteOnClick implements View.OnClickListener {
    private final NotesListActivity activity;

    public NewNoteOnClick(NotesListActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onClick(View v) {
        if (App.getAppContainer().getCryptoManager().getCryptoKeyInstance() != null) {
            activity.startActivity(new Intent(App.getContext(), OpenNoteActivity.class));
        }
    }
}
