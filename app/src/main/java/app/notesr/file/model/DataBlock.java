package app.notesr.file.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/** @noinspection LombokGetterMayBeUsed, LombokSetterMayBeUsed */
@Entity(
        tableName = "data_blocks",
        foreignKeys = @ForeignKey(
                entity = FileInfo.class,
                parentColumns = "id",
                childColumns = "file_id"
        ),
        indices = {@Index("file_id")}
)
@NoArgsConstructor
@AllArgsConstructor
public final class DataBlock {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    @ColumnInfo(name = "file_id")
    private String fileId;

    @NonNull
    @ColumnInfo(name = "block_order")
    private Long order;

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

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
