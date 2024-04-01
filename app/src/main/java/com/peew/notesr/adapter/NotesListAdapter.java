package com.peew.notesr.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.peew.notesr.R;
import com.peew.notesr.model.Note;

import java.util.List;
import java.util.Objects;

public class NotesListAdapter extends ArrayAdapter<Note> {
    private static final int MAX_VALUE_LENGTH = 25;
    private final int resourceLayout;
    private final Context context;

    public NotesListAdapter(Context context, int resource, List<Note> items) {
        super(context, resource, items);
        this.resourceLayout = resource;
        this.context = context;
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
            TextView nameView = (TextView) view.findViewById(R.id.note_name_text_view);
            TextView textView = (TextView) view.findViewById(R.id.note_text_view);

            nameView.setText(formatValue(note.name()));
            textView.setText(formatValue(note.text()));
        }

        return view;
    }

    @Override
    public long getItemId(int position) {
        return Objects.requireNonNull(getItem(position)).id();
    }

    private String formatValue(String value) {
        String formatted = value.replace("\n", "");
        return formatted.length() > MAX_VALUE_LENGTH ?
                formatted.substring(0, MAX_VALUE_LENGTH) + "…" : formatted;
    }
}
