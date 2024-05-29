package com.peew.notesr.db.notes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Environment;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peew.notesr.App;
import com.peew.notesr.crypto.Aes;
import com.peew.notesr.crypto.CryptoKey;
import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.model.Note;
import com.peew.notesr.model.NotesDatabaseDump;
import com.peew.notesr.tools.FileManager;
import com.peew.notesr.tools.VersionFetcher;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class NotesExporter {
    private final Context context;

    public NotesExporter(Context context) {
        this.context = context;
    }

    public String export() throws PackageManager.NameNotFoundException, IOException,
            InvalidAlgorithmParameterException, NoSuchPaddingException,
            IllegalBlockSizeException, NoSuchAlgorithmException,
            BadPaddingException, InvalidKeyException, MissingNotesException {
        NotesDatabaseDump dump = getDump();

        CryptoKey cryptoKey = App.getAppContainer().getCryptoManager().getCryptoKeyInstance();
        Aes aesInstance = new Aes(cryptoKey.key(), cryptoKey.salt());

        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        String jsonDump = mapper.writeValueAsString(dump);
        byte[] encryptedDump = aesInstance.encrypt(jsonDump.getBytes(StandardCharsets.UTF_8));

        File dumpFile = getDumpFile();
        FileManager.writeFileBytes(dumpFile, encryptedDump);

        return dumpFile.getPath();
    }

    private NotesDatabaseDump getDump() throws
            PackageManager.NameNotFoundException,
            MissingNotesException {
        List<Note> notes = NotesCrypt.decrypt(App.getAppContainer()
                .getNotesDatabase()
                .getNotesTable()
                .getAll());

        if (notes.isEmpty()) {
            throw new MissingNotesException();
        }

        String version = VersionFetcher.fetchVersionName(context, false);
        return new NotesDatabaseDump(version, notes);
    }

    @SuppressLint("SimpleDateFormat")
    private File getDumpFile() {
        Date now = Calendar.getInstance().getTime();

        String datetime = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(now);
        String filename = "nsr_export_" + datetime + ".notesr.bak";

        File filesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        Path dumpPath = Paths.get(filesDir.toPath().toString(), filename);

        return new File(dumpPath.toUri());
    }
}
