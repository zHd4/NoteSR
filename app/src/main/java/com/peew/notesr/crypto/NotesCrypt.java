package com.peew.notesr.crypto;

import android.util.Base64;
import android.util.Log;

import com.peew.notesr.model.EncryptedNote;
import com.peew.notesr.model.Note;

import java.nio.charset.StandardCharsets;

public class NotesCrypt {
    public EncryptedNote encrypt(Note note) {
        return encrypt(note, CryptoManager.getInstance().getCryptoKeyInstance());
    }

    public Note decrypt(EncryptedNote encryptedNote) {
        return decrypt(encryptedNote, CryptoManager.getInstance().getCryptoKeyInstance());
    }

    public EncryptedNote encrypt(Note note, CryptoKey cryptoKey) {
        String encryptedName = encryptValue(note.name(), cryptoKey);
        String encryptedText = encryptValue(note.text(), cryptoKey);

        return new EncryptedNote(note.id(), encryptedName, encryptedText);
    }

    public Note decrypt(EncryptedNote encryptedNote, CryptoKey cryptoKey) {
        String name = decryptValue(encryptedNote.encryptedName(), cryptoKey);
        String text = decryptValue(encryptedNote.encryptedText(), cryptoKey);

        return new Note(encryptedNote.id(), name, text);
    }

    private String encryptValue(String value, CryptoKey cryptoKey) {
        try {
            Aes aesInstance = getAesInstance(cryptoKey);
            byte[] encrypted = aesInstance.encrypt(value.getBytes(StandardCharsets.UTF_8));

            return Base64.encodeToString(encrypted, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e("Cannot encrypt note", e.toString());
            throw new RuntimeException(e);
        }
    }

    private String decryptValue(String value, CryptoKey cryptoKey) {
        try {
            Aes aesInstance = getAesInstance(cryptoKey);
            byte[] decoded = Base64.decode(value, Base64.DEFAULT);

            return new String(aesInstance.decrypt(decoded));
        } catch (Exception e) {
            Log.e("Cannot decrypt note", e.toString());
            throw new RuntimeException(e);
        }
    }

    private Aes getAesInstance(CryptoKey cryptoKey) {
        return new Aes(cryptoKey.key(), cryptoKey.salt());
    }
}
