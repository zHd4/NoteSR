package notesr.onclick.notes;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;

import notesr.App;
import notesr.activity.ExtendedAppCompatActivity;
import notesr.activity.notes.OpenNoteActivity;

public class OpenNoteOnClick implements AdapterView.OnItemClickListener {
    private final ExtendedAppCompatActivity activity;

    public OpenNoteOnClick(ExtendedAppCompatActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent noteOpenActivtyIntent = new Intent(App.getContext(), OpenNoteActivity.class);

        noteOpenActivtyIntent.putExtra("noteId", id);
        activity.startActivity(noteOpenActivtyIntent);
    }
}