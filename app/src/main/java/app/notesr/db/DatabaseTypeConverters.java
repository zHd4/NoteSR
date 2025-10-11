package app.notesr.db;

import android.net.Uri;

import androidx.room.TypeConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DatabaseTypeConverters {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @TypeConverter
    public static String fromLocalDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : FORMATTER.format(dateTime);
    }

    @TypeConverter
    public static LocalDateTime toLocalDateTime(String value) {
        return value == null ? null : LocalDateTime.parse(value, FORMATTER);
    }

    @TypeConverter
    public static String fromUri(Uri uri) {
        return uri == null ? null : uri.toString();
    }

    @TypeConverter
    public static Uri toUri(String uriString) {
        return uriString == null ? null : Uri.parse(uriString);
    }
}
