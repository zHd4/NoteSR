package app.notesr.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity(
        tableName = "files_blob_info",
        foreignKeys = @ForeignKey(
                entity = FileInfo.class,
                parentColumns = "id",
                childColumns = "file_id"
        ),
        indices = {@Index("file_id")}
)
@NoArgsConstructor
@AllArgsConstructor
public final class FileBlobInfo {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    @ColumnInfo(name = "file_id")
    @JsonProperty("file_id")
    private String fileId;

    @NonNull
    @ColumnInfo(name = "blob_order")
    private Long order;

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getFileId() {
        return fileId;
    }

    public void setFileId(@NonNull String fileId) {
        this.fileId = fileId;
    }

    @NonNull
    public Long getOrder() {
        return order;
    }

    public void setOrder(@NonNull Long order) {
        this.order = order;
    }
}
