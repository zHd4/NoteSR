package app.notesr.notes;

import android.content.Intent;
import android.view.View;

import app.notesr.App;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NewNoteOnClick implements View.OnClickListener {
    private final NoteListActivity activity;

    @Override
    public void onClick(View v) {
        if (App.getAppContainer().getCryptoManager().getCryptoKeyInstance() != null) {
            activity.startActivity(new Intent(App.getContext(), OpenNoteActivity.class));
        }
    }
}
