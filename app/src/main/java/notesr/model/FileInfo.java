package notesr.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class FileInfo implements Serializable {
    private Long id;
    private Long noteId;
    private Long size;
    private String name;
    private String type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
