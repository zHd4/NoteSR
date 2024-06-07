package com.peew.notesr.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.peew.notesr.R;
import com.peew.notesr.model.Note;

import java.util.List;
import java.util.Objects;

public class NotesListAdapter extends ElementsListAdapter<Note> {

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
            TextView nameView = view.findViewById(R.id.note_name_text_view);
            TextView textView = view.findViewById(R.id.note_text_view);
            TextView updatedAtView = view.findViewById(R.id.note_updated_at_text_view);

            nameView.setText(formatValue(note.getName()));
            textView.setText(formatValue(note.getText()));
            updatedAtView.setText(note.getUpdatedAt().format(timestampFormatter));
        }

        return view;
    }

    @Override
    public long getItemId(int position) {
        return Objects.requireNonNull(getItem(position)).getId();
    }
}
