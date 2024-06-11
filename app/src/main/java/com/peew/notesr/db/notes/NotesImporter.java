package com.peew.notesr.db.notes;

import android.content.pm.PackageManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peew.notesr.App;
import com.peew.notesr.crypto.Aes;
import com.peew.notesr.crypto.CryptoKey;
import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.model.Note;
import com.peew.notesr.model.NotesDatabaseDump;
import com.peew.notesr.tools.VersionFetcher;
import com.peew.notesr.activity.notes.ImportNotesActivity;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class NotesImporter {
    private final ImportNotesActivity activity;

    public NotesImporter(ImportNotesActivity activity) {
        this.activity = activity;
    }

    public NotesImportResult importDump(byte[] encryptedDump) {
        try {
            NotesDatabaseDump dump = decryptDump(encryptedDump);
            String currentVersionString = VersionFetcher.fetchVersionName(activity, true);

            int currentVersion = Integer.parseInt(currentVersionString);
            int dumpVersion = Integer.parseInt(dump.version().replace(".", ""));

            if (currentVersion < dumpVersion) {
                return NotesImportResult.INCOMPATIBLE_VERSION;
            }

            importNotes(dump.notes());
            return NotesImportResult.SUCCESS;
        } catch (IOException |
                InvalidAlgorithmParameterException | NoSuchPaddingException |
                IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException |
                InvalidKeyException e) {

            return NotesImportResult.INVALID_DUMP;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void importNotes(List<Note> notes) {
        NotesTable table = App.getAppContainer().getNotesDatabase().getNotesTable();

        notes.stream()
                .map(NotesCrypt::encrypt)
                .forEach(table::save);
    }

    private NotesDatabaseDump decryptDump(byte[] encryptedDump) throws IOException,
            InvalidAlgorithmParameterException, NoSuchPaddingException,
            IllegalBlockSizeException, NoSuchAlgorithmException,
            BadPaddingException, InvalidKeyException {
        ObjectMapper mapper = new ObjectMapper();

        CryptoKey cryptoKey = App.getAppContainer().getCryptoManager().getCryptoKeyInstance();
        Aes aesInstance = new Aes(cryptoKey.key(), cryptoKey.salt());

        byte[] dumpBytes = aesInstance.decrypt(encryptedDump);
        return mapper.readValue(dumpBytes, NotesDatabaseDump.class);
    }
}
