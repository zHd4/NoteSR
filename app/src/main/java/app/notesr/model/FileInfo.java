package app.notesr.model;

import androidx.room.Entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class FileInfo implements Serializable {
    private String id;
    private String noteId;
    private Long size;
    private String name;
    private String type;
    private byte[] thumbnail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long decimalId;
}
