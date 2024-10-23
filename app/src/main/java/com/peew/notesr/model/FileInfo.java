package com.peew.notesr.model;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Setter
@Getter
public class FileInfo implements Serializable {
    private Long id;

    @NotNull
    private Long noteId;

    @NotNull
    private Long size;

    @NotNull
    private String name;

    @NotNull
    private String type;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
