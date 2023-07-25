package com.notesr.controllers;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;
import com.notesr.models.Config;
import com.notesr.models.FileAttachment;
import com.notesr.models.Note;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

public class DatabaseController extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = Config.databaseName;

    private static final String TABLE_NOTES = "notes";
    private static final String KEY_TITLE = "title";
    private static final String KEY_TEXT = "text";

    private static final String TABLE_FILES = "files";
    private static final String KEY_FILE_ID = "id";
    private static final String KEY_NOTE_ID = "note_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_DATA_BASE64 = "data_base64";

    public DatabaseController(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                String.format("CREATE TABLE IF NOT EXISTS %s(%s TEXT, %s TEXT)",
                TABLE_NOTES,
                KEY_TITLE,
                KEY_TEXT)
        );

        db.execSQL(
                String.format("CREATE TABLE IF NOT EXISTS %s(%s INTEGER, %s INTEGER, %s TEXT, %s TEXT)",
                TABLE_FILES,
                KEY_FILE_ID,
                KEY_NOTE_ID,
                KEY_NAME,
                KEY_DATA_BASE64)
        );
    }

    private void clearNotes() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(String.format("DELETE FROM %s", TABLE_NOTES));
    }

    public Note[] getAllNotes() {
        Note[] notes = new Note[0];
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            @SuppressLint("Recycle")
            Cursor cursor = db.rawQuery(
                    String.format("SELECT * FROM %s", TABLE_NOTES),
                    null
            );

            if (cursor.moveToFirst()) {
                int id = 0;

                do {
                    notes = Arrays.copyOf(notes, notes.length + 1);
                    notes[notes.length - 1] = new Note(id, cursor.getString(0), cursor.getString(1));

                    id += 1;
                } while (cursor.moveToNext());
            }

            return notes;
        } catch (Exception e) {
            e.printStackTrace();
            return new Note[0];
        }
    }

    public void setAllNotes(Note[] notes) {
        clearNotes();

        for (int i = 0; i < notes.length; i++) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();

            values.put(KEY_TITLE, notes[i].getName());
            values.put(KEY_TEXT, notes[i].getText());

            db.insert(TABLE_NOTES, null, values);
            db.close();
        }
    }

    public void deleteNote(String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(String.format("DELETE FROM %s WHERE %s='%s'", TABLE_NOTES, KEY_TITLE, title));
    }

    public String exportToJsonString(Context context) throws Exception {
        JSONArray jsonNotes = new JSONArray();
        Note[] notes = NotesController.getNotes(context);

        for(int i = 0; i < notes.length; i++) {
            JSONObject note = new JSONObject();

            note.put("label", Base64.encodeToString(notes[i].getName().getBytes(), Base64.DEFAULT));
            note.put("text", Base64.encodeToString(notes[i].getText().getBytes(), Base64.DEFAULT));

            jsonNotes.put(note);
        }

        return jsonNotes.toString();
    }

    public void importFromJsonString(Context context, String jsonString) throws Exception {
        Note[] notes = new Note[0];
        JSONArray jsonNotes = new JSONArray(jsonString);

        for(int i = 0; i < jsonNotes.length(); i++) {
            JSONObject note = jsonNotes.getJSONObject(i);

            notes = Arrays.copyOf(notes, notes.length + 1);
            notes[notes.length - 1] = new Note(
                    i, new String(Base64.decode(note.getString("label"), Base64.DEFAULT)),
                    new String(Base64.decode(note.getString("text"), Base64.DEFAULT)));
        }

        NotesController.setNotes(context, notes);
    }

    public void addFile(FileAttachment file) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_FILE_ID, file.getId());
        values.put(KEY_NOTE_ID, file.getNoteId());
        values.put(KEY_NAME, file.getName());
        values.put(KEY_DATA_BASE64, Base64.encodeToString(file.getData(), Base64.DEFAULT));

        db.insert(TABLE_FILES, null, values);
        db.close();
    }

    public void deleteFile(FileAttachment file) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(String.format("DELETE FROM %s WHERE %s=%s", TABLE_FILES, KEY_NOTE_ID, file.getId()));
    }

    public int[] getFilesIdByNoteId(int note_id) {
        int[] files_id = new int[0];
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            @SuppressLint("Recycle")
            Cursor cursor = db.rawQuery(
                    String.format("SELECT %s FROM %s WHERE %s=%s", KEY_FILE_ID, TABLE_FILES, KEY_NOTE_ID, note_id),
                    null
            );

            if (cursor.moveToFirst()) {
                do {
                    files_id = Arrays.copyOf(files_id, files_id.length + 1);
                    files_id[files_id.length - 1] = Integer.parseInt(cursor.getString(0));
                } while (cursor.moveToNext());
            }

            return files_id;
        } catch (Exception e) {
            e.printStackTrace();

            return new int[0];
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
