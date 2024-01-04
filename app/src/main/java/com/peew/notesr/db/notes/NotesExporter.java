package com.peew.notesr.db.notes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Environment;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peew.notesr.crypto.Aes;
import com.peew.notesr.crypto.CryptoKey;
import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.models.NotesDatabaseDump;
import com.peew.notesr.tools.FileManager;
import com.peew.notesr.tools.VersionFetcher;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class NotesExporter {
    private final Context context;

    public NotesExporter(Context context) {
        this.context = context;
    }

    public String export() throws Exception {
        CryptoKey cryptoKey = CryptoManager.getInstance().getCryptoKeyInstance();
        Aes aesInstance = new Aes(cryptoKey.key(), cryptoKey.salt());

        NotesTable notesTable = NotesDatabase.getInstance().getNotesTable();
        String version = VersionFetcher.fetchVersionName(context, false);

        NotesDatabaseDump dump = new NotesDatabaseDump(version, notesTable.getAll());
        ObjectMapper mapper = new ObjectMapper();

        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        String jsonDump = mapper.writeValueAsString(dump);
        byte[] encryptedDump = aesInstance.encrypt(jsonDump.getBytes(StandardCharsets.UTF_8));

        File dumpFile = getDumpFile();
        FileManager.writeFileBytes(dumpFile, encryptedDump);

        return dumpFile.getPath();
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
