package app.notesr.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EncryptedFileThumbnail {
    private String id;
    private String fileId;
    private byte[] encryptedImage;
}
