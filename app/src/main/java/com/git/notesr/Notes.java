package com.git.notesr;

import android.content.Context;
import android.util.Base64;
//import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Notes {

    public static String[][] GetNotes(Context context) throws Exception {
        String encryptedJson = Storage.ReadFile(context, Config.notesJsonFileName);

        if(encryptedJson.equals("")) {
            return new String[0][0];
        } else {
            String jsonString =
                    AES.Decrypt(encryptedJson, Base64.decode(Config.aesKey, Base64.DEFAULT));
            JSONArray json = new JSONArray(jsonString);

            List<String> labels = new ArrayList<>();
            List<String> notesTexts = new ArrayList<>();

            for (int i=0; i<json.length(); i++) {
                JSONObject note = json.getJSONObject(i);

                String label =
                        new String(Base64.decode(note.getString("label"), Base64.DEFAULT));
                String text =
                        new String(Base64.decode(note.getString("text"), Base64.DEFAULT));

                labels.add(label);
                notesTexts.add(text);
            }

            String[][] result = new String[labels.size()][2];

            for (int i=0; i<result.length; i++) {
                result[i][0] = labels.get(i);
                result[i][1] = notesTexts.get(i);
            }

            return result;
        }
    }

    public static void SetNotes(Context context, String[][] notes) throws Exception {
        JSONArray jsonNotes = new JSONArray();

        for(int i=0; i<notes.length; i++) {
            JSONObject noteObj = new JSONObject();

            noteObj.put("label", Base64.encodeToString(notes[i][0].getBytes(),
                    Base64.DEFAULT));
            noteObj.put("text", Base64.encodeToString(notes[i][1].getBytes(),
                    Base64.DEFAULT));

            jsonNotes.put(noteObj);
        }

        String result = AES.Encrypt(jsonNotes.toString(), Base64.decode(Config.aesKey,
                Base64.DEFAULT));

        Storage.WriteFile(context, "notes.json", result);
    }
}