package app.notesr;

import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ElementListAdapter<T> extends ArrayAdapter<T> {

    protected static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    protected final int resourceLayout;
    protected final Context context;

    public ElementListAdapter(@NonNull Context context, int resource, @NonNull List<T> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resourceLayout = resource;
    }
}
