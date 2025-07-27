package app.notesr.model;

import androidx.room.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
public final class Note {
    @JsonProperty("id")
    private String id;

    @NotNull
    @JsonProperty("name")
    private String name;

    @NotNull
    @JsonProperty("text")
    private String text;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    private Long decimalId;
}
