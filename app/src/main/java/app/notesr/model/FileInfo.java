package app.notesr.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/** @noinspection LombokGetterMayBeUsed, LombokSetterMayBeUsed */
@Entity(
        tableName = "files_info",
        foreignKeys = @ForeignKey(
                entity = Note.class,
                parentColumns = "id",
                childColumns = "note_id"
        ),
        indices = {@Index("note_id")}
)
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo implements Serializable {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    @ColumnInfo(name = "note_id")
    private String noteId;

    @NonNull
    private Long size;

    @NonNull
    private String name;

    private String type;

    private byte[] thumbnail;

    @NonNull
    @ColumnInfo(name = "created_at")
    private LocalDateTime createdAt;

    @NonNull
    @ColumnInfo(name = "updated_at")
    private LocalDateTime updatedAt;

    @Ignore
    private Long decimalId;

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(@NonNull String noteId) {
        this.noteId = noteId;
    }

    @NonNull
    public Long getSize() {
        return size;
    }

    public void setSize(@NonNull Long size) {
        this.size = size;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public byte[] getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(byte[] thumbnail) {
        this.thumbnail = thumbnail;
    }

    @NonNull
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NonNull LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @NonNull
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(@NonNull LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getDecimalId() {
        return decimalId;
    }

    public void setDecimalId(Long decimalId) {
        this.decimalId = decimalId;
    }
}
