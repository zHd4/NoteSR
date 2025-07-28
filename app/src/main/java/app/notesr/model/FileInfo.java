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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@Getter
@Setter
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
}
