package app.notesr.cleaner.model;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/** @noinspection LombokGetterMayBeUsed, LombokSetterMayBeUsed */
@Entity(tableName = "temp_files")
@NoArgsConstructor
@AllArgsConstructor
public class TempFile {

    @PrimaryKey(autoGenerate = true)
    private Long id;

    @NonNull
    private Uri uri;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NonNull
    public Uri getUri() {
        return uri;
    }

    public void setUri(@NonNull Uri uri) {
        this.uri = uri;
    }
}
