package app.notesr.crypto;

import android.util.Log;
import app.notesr.App;
import app.notesr.dto.CryptoSecrets;
import app.notesr.model.EncryptedNote;
import app.notesr.model.Note;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class NoteCryptor {
    public static EncryptedNote updateKey(EncryptedNote note, CryptoSecrets oldKey, CryptoSecrets newKey) {
        return encrypt(decrypt(note, oldKey), newKey);
    }

    public static List<Note> decrypt(List<EncryptedNote> notes) {
        return decrypt(notes, getCryptoManager().getSecrets());
    }

    public static List<Note> decrypt(List<EncryptedNote> notes, CryptoSecrets cryptoKey) {
        return notes.stream().map(note -> decrypt(note, cryptoKey)).collect(Collectors.toList());
    }

    public static EncryptedNote encrypt(Note note) {
        return encrypt(note, getCryptoManager().getSecrets());
    }

    public static Note decrypt(EncryptedNote encryptedNote) {
        return decrypt(encryptedNote, getCryptoManager().getSecrets());
    }

    public static EncryptedNote encrypt(Note note, CryptoSecrets cryptoKey) {
        byte[] encryptedName = encryptValue(note.getName(), cryptoKey);
        byte[] encryptedText = encryptValue(note.getText(), cryptoKey);

        EncryptedNote encryptedNote = new EncryptedNote(encryptedName, encryptedText);
        encryptedNote.setUpdatedAt(note.getUpdatedAt());

        encryptedNote.setId(note.getId());
        return encryptedNote;
    }

    public static Note decrypt(EncryptedNote encryptedNote, CryptoSecrets cryptoKey) {
        String name = decryptValue(encryptedNote.getEncryptedName(), cryptoKey);
        String text = decryptValue(encryptedNote.getEncryptedText(), cryptoKey);

        Note note = new Note(name, text);
        note.setUpdatedAt(encryptedNote.getUpdatedAt());

        note.setId(encryptedNote.getId());
        return note;
    }

    private static byte[] encryptValue(String value, CryptoSecrets cryptoKey) {
        try {
            return getAesInstance(cryptoKey)
                    .encrypt(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.e("Cannot encrypt value", e.toString());
            throw new RuntimeException(e);
        }
    }

    private static String decryptValue(byte[] value, CryptoSecrets cryptoKey) {
        try {
            byte[] decrypted = getAesInstance(cryptoKey).decrypt(value);
            return new String(decrypted);
        } catch (Exception e) {
            Log.e("Cannot decrypt value", e.toString());
            throw new RuntimeException(e);
        }
    }

    private static AesCryptor getAesInstance(CryptoSecrets cryptoKey) {
        return new AesCryptor(cryptoKey.getKey(), cryptoKey.getSalt());
    }
    
    private static CryptoManager getCryptoManager() {
        return App.getAppContainer().getCryptoManager();
    }
}
