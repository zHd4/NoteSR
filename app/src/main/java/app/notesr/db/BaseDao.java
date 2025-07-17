package app.notesr.db;

import android.database.Cursor;
import app.notesr.App;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@AllArgsConstructor
public abstract class BaseDao {

    protected final BaseDb db;

    @Getter
    protected final String name;

    public long getRowsCount() {
        Cursor cursor = db.readableDatabase.rawQuery("SELECT COUNT(*) FROM " + getName(), null);
        Long count = null;

        try (cursor) {
            if (cursor.moveToFirst()) {
                count = cursor.getLong(0);
            }
        }

        if (count == null) {
            throw new NullPointerException();
        }

        return count;
    }

    public void deleteAll() {
        db.writableDatabase.delete(name, null, null);
    }

    protected DateTimeFormatter getTimestampFormatter() {
        return App.getAppContainer().getTimestampFormatter();
    }
}
