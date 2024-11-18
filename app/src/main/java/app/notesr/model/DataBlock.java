package app.notesr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class DataBlock {
    private Long id;
    private Long fileId;
    private Long order;
    private byte[] data;
}
