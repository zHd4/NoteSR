package app.notesr.crypto;

import android.util.Log;
import app.notesr.App;
import app.notesr.dto.CryptoKey;
import app.notesr.model.EncryptedNote;
import app.notesr.model.Note;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class NoteCryptor {
    public static EncryptedNote updateKey(EncryptedNote note, CryptoKey oldKey, CryptoKey newKey) {
        return encrypt(decrypt(note, oldKey), newKey);
    }

    public static List<Note> decrypt(List<EncryptedNote> notes) {
        return decrypt(notes, getCryptoManager().getCryptoKeyInstance());
    }

    public static List<Note> decrypt(List<EncryptedNote> notes, CryptoKey cryptoKey) {
        return notes.stream().map(note -> decrypt(note, cryptoKey)).collect(Collectors.toList());
    }

    public static EncryptedNote encrypt(Note note) {
        return encrypt(note, getCryptoManager().getCryptoKeyInstance());
    }

    public static Note decrypt(EncryptedNote encryptedNote) {
        return decrypt(encryptedNote, getCryptoManager().getCryptoKeyInstance());
    }

    public static EncryptedNote encrypt(Note note, CryptoKey cryptoKey) {
        byte[] encryptedName = encryptValue(note.getName(), cryptoKey);
        byte[] encryptedText = encryptValue(note.getText(), cryptoKey);

        EncryptedNote encryptedNote = new EncryptedNote(encryptedName, encryptedText);
        encryptedNote.setUpdatedAt(note.getUpdatedAt());

        encryptedNote.setId(note.getId());
        return encryptedNote;
    }

    public static Note decrypt(EncryptedNote encryptedNote, CryptoKey cryptoKey) {
        String name = decryptValue(encryptedNote.getEncryptedName(), cryptoKey);
        String text = decryptValue(encryptedNote.getEncryptedText(), cryptoKey);

        Note note = new Note(name, text);
        note.setUpdatedAt(encryptedNote.getUpdatedAt());

        note.setId(encryptedNote.getId());
        return note;
    }

    private static byte[] encryptValue(String value, CryptoKey cryptoKey) {
        try {
            return getAesInstance(cryptoKey)
                    .encrypt(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.e("Cannot encrypt value", e.toString());
            throw new RuntimeException(e);
        }
    }

    private static String decryptValue(byte[] value, CryptoKey cryptoKey) {
        try {
            byte[] decrypted = getAesInstance(cryptoKey).decrypt(value);
            return new String(decrypted);
        } catch (Exception e) {
            Log.e("Cannot decrypt value", e.toString());
            throw new RuntimeException(e);
        }
    }

    private static AesCryptor getAesInstance(CryptoKey cryptoKey) {
        return new AesCryptor(cryptoKey.getKey(), cryptoKey.getSalt());
    }
    
    private static CryptoManager getCryptoManager() {
        return App.getAppContainer().getCryptoManager();
    }
}
