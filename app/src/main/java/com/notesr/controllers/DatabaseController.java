package com.notesr.controllers;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;
import com.notesr.models.Config;
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
                do {
                    notes = Arrays.copyOf(notes, notes.length + 1);
                    notes[notes.length - 1] = new Note(cursor.getString(0), cursor.getString(1));
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
                    new String(Base64.decode(note.getString("label"), Base64.DEFAULT)),
                    new String(Base64.decode(note.getString("text"), Base64.DEFAULT)));
        }

        NotesController.setNotes(context, notes);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
