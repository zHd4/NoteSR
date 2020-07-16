package com.git.notesr;

import android.content.Context;
import android.util.Base64;

public class Notes {

    public static String[][] getNotes(Context context) throws Exception {
        Database db = new Database(context);
        String[][] notes = db.getAllNotes();

        if(!notes.equals(new String[0][0]) && Config.aesKey != null) {
            byte[] key = Base64.decode(Config.aesKey, Base64.DEFAULT);

            for(int i = 0; i < notes.length; i++) {
                notes[i][0] = AES.decrypt(notes[i][0], key);
                notes[i][1] = AES.decrypt(notes[i][1], key);
            }

            return notes;
        } else {
            return new String[0][0];
        }
    }

    public static void setNotes(Context context, String[][] notes) throws Exception {
        Database db = new Database(context);
        byte[] key = Base64.decode(Config.aesKey, Base64.DEFAULT);

        for(int i = 0; i < notes.length; i++) {
            notes[i][0] = AES.encrypt(notes[i][0], key);
            notes[i][1] = AES.encrypt(notes[i][1], key);
        }

        db.setAllNotes(notes);
    }
}