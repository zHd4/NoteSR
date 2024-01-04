package com.peew.notesr.db.notes;

import android.content.pm.PackageManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peew.notesr.crypto.Aes;
import com.peew.notesr.crypto.CryptoKey;
import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.models.Note;
import com.peew.notesr.models.NotesDatabaseDump;
import com.peew.notesr.tools.VersionFetcher;
import com.peew.notesr.ui.manage.ImportNotesActivity;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

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
            String version = VersionFetcher.fetchVersionName(activity, false);
            NotesDatabaseDump dump = decryptDump(encryptedDump);

            if (!Objects.equals(version, dump.version())) {
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
        NotesTable table = NotesDatabase.getInstance().getNotesTable();
        Consumer<Note> importer = note -> {
            if (table.exists(note.id())) {
                if (!table.get(note.id()).equals(note)) {
                    table.update(note);
                }

            } else {
                table.add(note);
            }
        };

        notes.forEach(importer);
    }

    private NotesDatabaseDump decryptDump(byte[] encryptedDump) throws IOException,
            InvalidAlgorithmParameterException, NoSuchPaddingException,
            IllegalBlockSizeException, NoSuchAlgorithmException,
            BadPaddingException, InvalidKeyException {
        ObjectMapper mapper = new ObjectMapper();

        CryptoKey cryptoKey = CryptoManager.getInstance().getCryptoKeyInstance();
        Aes aesInstance = new Aes(cryptoKey.key(), cryptoKey.salt());

        byte[] dumpBytes = aesInstance.decrypt(encryptedDump);
        return mapper.readValue(dumpBytes, NotesDatabaseDump.class);
    }
}
