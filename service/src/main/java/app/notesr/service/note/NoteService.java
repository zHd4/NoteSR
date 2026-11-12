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

/**
 * Provides CRUD-style operations for notes and manages their persistence lifecycle.
 *
 * <p>This service coordinates interaction with the application's database layer,
 * handling note creation, retrieval, searching, deletion, and import workflows while
 * also maintaining the derived decimal identifier used by the rest of the system.</p>
 */
@RequiredArgsConstructor
public class NoteService {
    private final AppDatabase db;

    /**
     * Persists a note, creating identifiers and timestamps when they are missing.
     *
     * <p>If the note has no ID, a new UUID is generated. If either timestamp is absent,
     * the current time is assigned before inserting or updating the record in the database.</p>
     *
     * @param note the note to save; must not be null
     */
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

    /**
     * Retrieves all notes from the database and enriches each with its decimal ID.
     *
     * @return a list of all notes, each updated with a derived decimal identifier
     */
    public List<Note> getAll() {
        return db.getNoteDao()
                .getAll()
                .stream()
                .map(this::setDecimalId)
                .collect(Collectors.toList());
    }

    /**
     * Fetches a single note by its identifier.
     *
     * @param id the unique note identifier
     * @return the matching note with its decimal identifier set, or null if no note exists
     */
    public Note get(String id) {
        Note note = db.getNoteDao().get(id);

        if (note != null) {
            setDecimalId(note);
            return note;
        }

        return null;
    }

    /**
     * Searches for notes matching the provided query text.
     *
     * @param query the search string to match against notes; leading and trailing
     *              whitespace is trimmed
     * @return a list of matching notes, each with its decimal identifier assigned
     */
    public List<Note> search(String query) {
        return db.getNoteDao()
                .search(query.trim())
                .stream()
                .map(this::setDecimalId)
                .collect(Collectors.toList());
    }

    /**
     * Deletes a note and any associated files attached to it.
     *
     * <p>The operation runs inside a database transaction so that file removals and the
     * note deletion remain consistent. If a file deletion fails, the underlying IO exception
     * is rethrown.</p>
     *
     * @param id the identifier of the note to delete
     * @param fileService the file service used to delete files associated with the note
     * @throws IOException if deleting one of the associated files fails
     */
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

    /**
     * Imports a note by inserting it into storage with default timestamps when required.
     *
     * <p>Unlike {@link #save(Note)}, this method always inserts a note record and does not
     * perform an update-or-insert check.</p>
     *
     * @param note the note to import; if missing timestamps, they are populated before insertion
     */
    public void importNote(Note note) {
        if (note.getUpdatedAt() == null) {
            note.setUpdatedAt(LocalDateTime.now());
        }

        if (note.getCreatedAt() == null) {
            note.setCreatedAt(note.getUpdatedAt());
        }

        db.getNoteDao().insert(note);
    }

    /**
     * Counts the number of stored notes in the database.
     *
     * @return the total number of note rows
     */
    public long getCount() {
        return db.getNoteDao().getRowsCount();
    }

    /**
     * Converts the note's UUID into the derived decimal identifier stored on the entity.
     *
     * @param note the note to enrich with its decimal identifier
     * @return the same note instance with its decimal ID set
     */
    private Note setDecimalId(Note note) {
        UUID uuid = UUID.fromString(note.getId());
        long hash = HashUtils.getUUIDHash(uuid);

        note.setDecimalId(hash);

        return note;
    }
}
