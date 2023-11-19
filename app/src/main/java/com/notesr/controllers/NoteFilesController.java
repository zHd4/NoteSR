package com.notesr.controllers;

import android.content.Context;
import android.util.Base64;

import com.notesr.controllers.crypto.CryptoController;
import com.notesr.controllers.db.DatabaseController;
import com.notesr.controllers.managers.HashManager;
import com.notesr.models.Config;
import com.notesr.models.FileAttachment;

import java.util.Dictionary;
import java.util.Hashtable;

/** @noinspection resource*/
public class NoteFilesController {
    public static Dictionary<Integer, String> getAttachments(Context context, int noteId) {
        Dictionary<Integer, String> result = new Hashtable<>();
        DatabaseController db = new DatabaseController(context);

        int[] notesId = db.getFilesIdByNoteId(noteId);

        for(int id = 0; id < notesId.length; id++) {
            String name = db.getFileNameById(id);
            result.put(id, name);
        }

        return result;
    }

    public static void addFile(Context context, FileAttachment file) throws Exception {
        DatabaseController db = new DatabaseController(context);

        byte[] key = Base64.decode(Config.cryptoKey, Base64.DEFAULT);
        byte[] encryptedData = CryptoController.encrypt(file.getData(),
                HashManager.toSha256String(Config.cryptoKey), key);

        file.setData(encryptedData);
        db.addFile(file);
    }

    public static FileAttachment getFile(Context context, int fileId) throws Exception {
        DatabaseController db = new DatabaseController(context);
        FileAttachment file = db.getFileById(fileId);

        byte[] key = Base64.decode(Config.cryptoKey, Base64.DEFAULT);
        byte[] decryptedData = CryptoController.decrypt(file.getData(),
                HashManager.toSha256String(Config.cryptoKey), key);

        file.setData(decryptedData);

        return file;
    }

    public void deleteFile(Context context, FileAttachment file) {
        DatabaseController db = new DatabaseController(context);
        db.deleteFile(file);
    }
}
