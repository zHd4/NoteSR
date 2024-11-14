package app.notesr.model;

import org.jetbrains.annotations.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@RequiredArgsConstructor
@Setter
@Getter
public class DataBlock {
    private Long id;

    @NotNull
    private Long fileId;

    @NotNull
    private Long order;

    @NotNull
    private byte[] data;
}
