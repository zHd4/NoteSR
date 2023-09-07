package com.notesr.controllers;

import android.content.Context;
import android.util.Base64;
import com.notesr.models.Config;
import com.notesr.models.Note;

/** @noinspection resource*/
public class NotesController {

    public static Note[] getNotes(Context context) throws Exception {
        DatabaseController db = new DatabaseController(context);
        Note[] notes = db.getAllNotes();

        if(!notes.equals(new Note[0]) && Config.cryptoKey != null) {
            byte[] key = Base64.decode(Config.cryptoKey, Base64.DEFAULT);

            for(int i = 0; i < notes.length; i++) {
                String decryptedName = new String(
                        CryptoController.decrypt(Base64.decode(notes[i].getName(), Base64.DEFAULT),
                                ActivityTools.sha256(Config.cryptoKey), key));

                String decryptedText = new String(
                        CryptoController.decrypt(Base64.decode(notes[i].getText(), Base64.DEFAULT),
                                ActivityTools.sha256(Config.cryptoKey), key));

                notes[i].setName(decryptedName);
                notes[i].setText(decryptedText);
            }

            return notes;
        } else {
            return new Note[0];
        }
    }

    public static void setNotes(Context context, final Note[] notes) throws Exception {
        DatabaseController db = new DatabaseController(context);
        byte[] key = Base64.decode(Config.cryptoKey, Base64.DEFAULT);

        for(int i = 0; i < notes.length; i++) {
            String name = Base64.encodeToString(
                    CryptoController.encrypt(
                            notes[i].getName().getBytes(),
                            ActivityTools.sha256(Config.cryptoKey),
                            key),
                    Base64.DEFAULT);

            String text = Base64.encodeToString(
                    CryptoController.encrypt(
                            notes[i].getText().getBytes(),
                            ActivityTools.sha256(Config.cryptoKey),
                            key),
                    Base64.DEFAULT);

            notes[i] = new Note(i, name, text);
        }

        db.setAllNotes(notes);
    }
}