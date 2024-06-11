package com.peew.notesr.onclick.notes;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;

import com.peew.notesr.App;
import com.peew.notesr.activity.AppCompatActivityExtended;
import com.peew.notesr.activity.notes.NoteOpenActivity;

public class NoteOnClick implements AdapterView.OnItemClickListener {
    private final AppCompatActivityExtended activity;

    public NoteOnClick(AppCompatActivityExtended activity) {
        this.activity = activity;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent noteOpenActivtyIntent = new Intent(App.getContext(), NoteOpenActivity.class);

        noteOpenActivtyIntent.putExtra("note_id", id);
        activity.startActivity(noteOpenActivtyIntent);
    }
}
