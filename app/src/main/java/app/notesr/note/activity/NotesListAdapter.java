package app.notesr.note.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import app.notesr.ElementListAdapter;
import app.notesr.R;
import app.notesr.note.model.Note;

import java.util.List;
import java.util.Objects;

public class NotesListAdapter extends ElementListAdapter<Note> {

    public NotesListAdapter(Context context, int resource, List<Note> items) {
        super(context, resource, items);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            LayoutInflater inflater;
            inflater = LayoutInflater.from(context);
            view = inflater.inflate(resourceLayout, null);
        }

        Note note = getItem(position);

        if (note != null) {
            TextView nameView = view.findViewById(R.id.noteNameTextView);
            TextView textView = view.findViewById(R.id.noteTextView);
            TextView updatedAtView = view.findViewById(R.id.noteUpdatedAtTextView);

            nameView.setText(note.getName());
            textView.setText(note.getText());
            updatedAtView.setText(note.getUpdatedAt().format(timestampFormatter));
        }

        return view;
    }

    @Override
    public long getItemId(int position) {
        return Objects.requireNonNull(getItem(position)).getDecimalId();
    }
}
