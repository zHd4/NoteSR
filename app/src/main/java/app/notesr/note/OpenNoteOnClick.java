package app.notesr.note;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;

import java.util.Map;

import app.notesr.App;
import app.notesr.ActivityBase;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OpenNoteOnClick implements AdapterView.OnItemClickListener {
    private final ActivityBase activity;
    private final Map<Long, String> notesIdsMap;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String noteId = notesIdsMap.get(id);
        Intent noteOpenActivtyIntent = new Intent(App.getContext(), OpenNoteActivity.class);

        noteOpenActivtyIntent.putExtra("noteId", noteId);
        activity.startActivity(noteOpenActivtyIntent);
    }
}
