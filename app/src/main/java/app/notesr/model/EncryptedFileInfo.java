package app.notesr.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Builder
@Setter
@Getter
public class EncryptedFileInfo {
    private Long id;
    private Long noteId;
    private Long size;
    private byte[] encryptedName;
    private byte[] encryptedType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
