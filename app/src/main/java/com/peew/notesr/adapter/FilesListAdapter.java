package com.peew.notesr.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.peew.notesr.R;
import com.peew.notesr.model.File;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;

public class FilesListAdapter extends ElementsListAdapter<File> {
    private static String toReadableSize(long size) {
        String[] units = new String[] { "B", "KB", "MB", "GB", "TB", "PB", "EB" };

        if(size > 0) {
            int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
            return new DecimalFormat("#,##0.#")
                    .format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
        }

        return "0";
    }


    public FilesListAdapter(@NonNull Context context, int resource, @NonNull List<File> objects) {
        super(context, resource, objects);
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

        File file = getItem(position);

        if (file != null) {
            TextView nameView = view.findViewById(R.id.file_name_text_view);
            TextView sizeView = view.findViewById(R.id.file_size_text_view);

            nameView.setText(formatValue(file.getName()));
            sizeView.setText(formatValue(toReadableSize(file.getData().length)));
        }

        return view;
    }

    @Override
    public long getItemId(int position) {
        return Objects.requireNonNull(getItem(position)).getId();
    }
}
