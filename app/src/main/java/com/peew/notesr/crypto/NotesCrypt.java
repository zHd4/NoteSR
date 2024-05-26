package com.peew.notesr.crypto;

import android.util.Base64;
import android.util.Log;

import com.peew.notesr.model.EncryptedNote;
import com.peew.notesr.model.Note;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class NotesCrypt {
    public static List<EncryptedNote> updateKey(List<EncryptedNote> notes,
                                                CryptoKey oldKey,
                                                CryptoKey newKey) {
        return notes.stream()
                .map(note -> encrypt(decrypt(note, oldKey), newKey))
                .collect(Collectors.toList());
    }

    public static List<Note> decrypt(List<EncryptedNote> notes) {
        return decrypt(notes, CryptoManager.getInstance().getCryptoKeyInstance());
    }

    public static List<Note> decrypt(List<EncryptedNote> notes, CryptoKey cryptoKey) {
        return notes.stream().map(note -> decrypt(note, cryptoKey)).collect(Collectors.toList());
    }

    public static EncryptedNote encrypt(Note note) {
        return encrypt(note, CryptoManager.getInstance().getCryptoKeyInstance());
    }

    public static Note decrypt(EncryptedNote encryptedNote) {
        return decrypt(encryptedNote, CryptoManager.getInstance().getCryptoKeyInstance());
    }

    public static EncryptedNote encrypt(Note note, CryptoKey cryptoKey) {
        String encryptedName = encryptValue(note.name(), cryptoKey);
        String encryptedText = encryptValue(note.text(), cryptoKey);

        return new EncryptedNote(note.id(), encryptedName, encryptedText);
    }

    public static Note decrypt(EncryptedNote encryptedNote, CryptoKey cryptoKey) {
        String name = decryptValue(encryptedNote.encryptedName(), cryptoKey);
        String text = decryptValue(encryptedNote.encryptedText(), cryptoKey);

        return new Note(encryptedNote.id(), name, text);
    }

    private static String encryptValue(String value, CryptoKey cryptoKey) {
        try {
            Aes aesInstance = getAesInstance(cryptoKey);
            byte[] encrypted = aesInstance.encrypt(value.getBytes(StandardCharsets.UTF_8));

            return Base64.encodeToString(encrypted, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e("Cannot encrypt note", e.toString());
            throw new RuntimeException(e);
        }
    }

    private static String decryptValue(String value, CryptoKey cryptoKey) {
        try {
            Aes aesInstance = getAesInstance(cryptoKey);
            byte[] decoded = Base64.decode(value, Base64.DEFAULT);

            return new String(aesInstance.decrypt(decoded));
        } catch (Exception e) {
            Log.e("Cannot decrypt note", e.toString());
            throw new RuntimeException(e);
        }
    }

    private static Aes getAesInstance(CryptoKey cryptoKey) {
        return new Aes(cryptoKey.key(), cryptoKey.salt());
    }
}
