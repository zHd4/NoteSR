package com.peew.notesr.onclick.notes;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;

import com.peew.notesr.App;
import com.peew.notesr.activity.ExtendedAppCompatActivity;
import com.peew.notesr.activity.notes.OpenNoteActivity;

public class OpenNoteOnClick implements AdapterView.OnItemClickListener {
    private final ExtendedAppCompatActivity activity;

    public OpenNoteOnClick(ExtendedAppCompatActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent noteOpenActivtyIntent = new Intent(App.getContext(), OpenNoteActivity.class);

        noteOpenActivtyIntent.putExtra("note_id", id);
        activity.startActivity(noteOpenActivtyIntent);
    }
}
