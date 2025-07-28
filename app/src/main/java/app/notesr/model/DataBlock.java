package app.notesr.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity(
        tableName = "data_block",
        foreignKeys = @ForeignKey(
                entity = FileInfo.class,
                parentColumns = "id",
                childColumns = "file_id"
        ),
        indices = {@Index("file_id")}
)
@NoArgsConstructor
@AllArgsConstructor
public class DataBlock {

    @PrimaryKey
    @NonNull
    private String id;

    @ColumnInfo(name = "file_id")
    @NonNull
    private String fileId;

    @ColumnInfo(name = "block_order")
    @NonNull
    private Long order;

    @NonNull
    private byte[] data;

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

    @NonNull
    public byte[] getData() {
        return data;
    }

    public void setData(@NonNull byte[] data) {
        this.data = data;
    }
}
