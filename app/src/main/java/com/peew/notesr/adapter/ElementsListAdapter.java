package com.peew.notesr.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import java.util.List;

public class ElementsListAdapter<T> extends ArrayAdapter<T> {
    private static final int MAX_VALUE_LENGTH = 25;

    protected final int resourceLayout;
    protected final Context context;

    public ElementsListAdapter(@NonNull Context context, int resource, @NonNull List<T> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resourceLayout = resource;
    }

    protected String formatValue(String value) {
        String formatted = value.replace("\n", "");
        return formatted.length() > MAX_VALUE_LENGTH ?
                formatted.substring(0, MAX_VALUE_LENGTH) + "…" : formatted;
    }
}
