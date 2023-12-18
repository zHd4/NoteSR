package com.peew.notesr.db.notes.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.peew.notesr.models.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class NotesTable extends Table {
    private final String name = "notes";
    private final Map<String, String> fields = Map.of(
            NotesTableField.NOTE_ID.getName(), "INTEGER",
            NotesTableField.ENCRYPTED_NAME.getName(), "TEXT",
            NotesTableField.ENCRYPTED_DATA.getName(), "TEXT"
            );

    public NotesTable(SQLiteOpenHelper helper) {
        super(helper);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, String> getFields() {
        return fields;
    }

    public void add(Note note) {
        try (SQLiteDatabase db = helper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(NotesTableField.NOTE_ID.getName(), note.getId());

            values.put(NotesTableField.ENCRYPTED_NAME.getName(), encrypt(note.getName()));
            values.put(NotesTableField.ENCRYPTED_DATA.getName(), encrypt(note.getText()));

            db.insert(name, null, values);
        }
    }

    public boolean exists(long id) {
        boolean exists;

        SQLiteDatabase db = helper.getReadableDatabase();
        String idFieldName = NotesTableField.NOTE_ID.getName();

        Cursor cursor = db.query(name,
                new String[] { idFieldName },
                idFieldName + "=" + id,
                null,
                null,
                null,
                null);

        try (db) {
            try (cursor){
                exists = cursor.moveToFirst();
            }
        }

        return exists;
    }

    public List<Note> getAll() {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Note> notes = new ArrayList<>();

        Cursor cursor = db.query(name,
                null,
                null,
                null,
                null,
                null,
                null);

        try (db) {
            try (cursor) {
                if (cursor.moveToFirst()) {
                    do {
                        long id = cursor.getLong(0);

                        String name = decrypt(cursor.getString(1));
                        String text = decrypt(cursor.getString(2));

                        notes.add(new Note(id, name, text));
                    } while (cursor.moveToNext());
                }
            }
        }

        return notes;
    }

    public Note get(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String idFieldName = NotesTableField.NOTE_ID.getName();

        Cursor cursor = db.query(name,
                null,
                idFieldName + "=" + id,
                null,
                null,
                null,
                null);

        try (db) {
            try (cursor) {
                if (cursor.moveToFirst()) {
                    String name = decrypt(cursor.getString(1));
                    String text = decrypt(cursor.getString(2));

                    return new Note(id, name, text);
                } else {
                    throw new RuntimeException("Wrong note id");
                }
            }
        }
    }

    public long getNewNoteId() {
        long newId = 0;

        SQLiteDatabase db = helper.getReadableDatabase();
        String idFieldName = NotesTableField.NOTE_ID.getName();

        Cursor cursor = db.query(name,
                null,
                null,
                null,
                null,
                null,
                idFieldName + " DESC LIMIT 1");

        try (db) {
            try (cursor) {
                if (cursor.moveToFirst()) {
                    newId = Long.parseLong(decrypt(cursor.getString(0))) + 1;
                }
            }
        }

        return newId;
    }

    public void update(Note oldNote, Note newNote) {
        if (oldNote.getId() != newNote.getId()) {
            throw new RuntimeException("Old note id not equal new note id");
        }

        try (SQLiteDatabase db = helper.getWritableDatabase()) {
            ContentValues values = new ContentValues();

            values.put(NotesTableField.ENCRYPTED_NAME.getName(), encrypt(newNote.getName()));
            values.put(NotesTableField.ENCRYPTED_DATA.getName(), encrypt(newNote.getText()));

            String whereClause = NotesTableField.NOTE_ID.getName() + "=" + oldNote.getId();
            db.update(name, values, whereClause, null);
        }
    }

    public void delete(Note note) {
        try (SQLiteDatabase db = helper.getWritableDatabase()) {
            String idFieldName = NotesTableField.NOTE_ID.name();

            db.execSQL("DELETE FROM " + name + " WHERE " + idFieldName + "='" + note.getId());
        }
    }

    private enum NotesTableField {
        NOTE_ID("note_id"),
        ENCRYPTED_NAME("encrypted_name"),
        ENCRYPTED_DATA("encrypted_data");

        private final String name;

        NotesTableField(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
