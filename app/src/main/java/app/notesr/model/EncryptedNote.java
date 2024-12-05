package app.notesr.model;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
public final class EncryptedNote {
    @Setter
    private String id;

    @NotNull
    private final byte[] encryptedName;

    @NotNull
    private final byte[] encryptedText;

    @Setter
    private LocalDateTime updatedAt;
}
