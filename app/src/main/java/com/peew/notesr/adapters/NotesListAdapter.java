package com.peew.notesr.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.peew.notesr.R;
import com.peew.notesr.models.NoteItem;

import java.util.List;

public class NotesListAdapter extends ArrayAdapter<NoteItem> {
    private static final int MAX_VALUE_LENGTH = 25;
    private final int resourceLayout;
    private final Context context;

    public NotesListAdapter(Context context, int resource, List<NoteItem> items) {
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

        NoteItem item = getItem(position);

        if (item != null) {
            TextView nameView = (TextView) view.findViewById(R.id.note_name_text_view);
            TextView textView = (TextView) view.findViewById(R.id.note_text_view);

            if (nameView != null) {
                nameView.setText(formatValue(item.name()));
            }

            if (textView != null) {
                textView.setText(formatValue(item.text()));
            }
        }

        return view;
    }

    private String formatValue(String value) {
        String formatted = value.replace("\n", "");
        return formatted.length() > MAX_VALUE_LENGTH ?
                formatted.substring(0, MAX_VALUE_LENGTH) + "…" : formatted;
    }
}
