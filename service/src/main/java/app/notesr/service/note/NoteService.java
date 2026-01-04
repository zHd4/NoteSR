/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.note;

import static java.util.Objects.requireNonNull;

import app.notesr.core.util.HashUtils;
import app.notesr.data.AppDatabase;
import app.notesr.data.model.FileInfo;
import app.notesr.data.model.Note;
import app.notesr.service.file.FileService;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public final class NoteService {
    private final AppDatabase db;

    public void save(Note note) {
        if (note.getId() == null) {
            note.setId(UUID.randomUUID().toString());
        }

        if (note.getCreatedAt() == null) {
            note.setCreatedAt(LocalDateTime.now());
        }

        if (note.getUpdatedAt() == null) {
            note.setUpdatedAt(LocalDateTime.now());
        }

        if (db.getNoteDao().get(note.getId()) == null) {
            db.getNoteDao().insert(note);
        } else {
            db.getNoteDao().update(note);
        }
    }

    public List<Note> getAll() {
        return db.getNoteDao()
                .getAll()
                .stream()
                .map(this::setDecimalId)
                .collect(Collectors.toList());
    }

    public Note get(String id) {
        Note note = db.getNoteDao().get(id);

        if (note != null) {
            setDecimalId(note);
            return note;
        }

        return null;
    }

    public List<Note> search(String query) {
        return db.getNoteDao()
                .search(query.trim())
                .stream()
                .map(this::setDecimalId)
                .collect(Collectors.toList());
    }

    public void delete(String id, FileService fileService) throws IOException {
        try {
            db.runInTransaction(() -> {
                for (FileInfo fileInfo : db.getFileInfoDao().getByNoteId(id)) {
                    try {
                        fileService.delete(fileInfo.getId());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }

                db.getNoteDao().deleteById(id);
            });
        } catch (UncheckedIOException e) {
            throw requireNonNull(e.getCause());
        }
    }

    public void importNote(Note note) {
        if (note.getUpdatedAt() == null) {
            note.setUpdatedAt(LocalDateTime.now());
        }

        if (note.getCreatedAt() == null) {
            note.setCreatedAt(note.getUpdatedAt());
        }

        db.getNoteDao().insert(note);
    }

    public long getCount() {
        return db.getNoteDao().getRowsCount();
    }

    private Note setDecimalId(Note note) {
        UUID uuid = UUID.fromString(note.getId());
        long hash = HashUtils.getUUIDHash(uuid);

        note.setDecimalId(hash);

        return note;
    }
}
