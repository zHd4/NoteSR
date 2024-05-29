package com.peew.notesr.ui.onclick;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;

import com.peew.notesr.App;
import com.peew.notesr.ui.ExtendedAppCompatActivity;
import com.peew.notesr.ui.manage.NoteOpenActivity;

public class NoteOnClick implements AdapterView.OnItemClickListener {
    private final ExtendedAppCompatActivity activity;

    public NoteOnClick(ExtendedAppCompatActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent noteOpenActivtyIntent = new Intent(App.getContext(), NoteOpenActivity.class);

        noteOpenActivtyIntent.putExtra("note_id", id);
        activity.startActivity(noteOpenActivtyIntent);
    }
}
