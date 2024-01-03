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
    private static final String DUMP_DIRECTORY_NAME = "NoteSR_exports";
    private final Context context;

    public NotesExporter(Context context) {
        this.context = context;
    }

    public void export() throws Exception {
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
    }

    @SuppressLint("SimpleDateFormat")
    private File getDumpFile() {
        Date now = Calendar.getInstance().getTime();

        String datetime = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(now);
        String filename = "nsr_export_" + datetime + ".notesr.bak";

        String homePath = Environment.getExternalStorageDirectory().toString();

        Path dumpsDirectoryPath = Paths.get(homePath, DUMP_DIRECTORY_NAME);
        File dumpsDirectory = new File(dumpsDirectoryPath.toUri());

        if (!FileManager.directoryExists(dumpsDirectory)) {
            FileManager.createDirectory(dumpsDirectory);
        }

        Path dumpPath = Paths.get(homePath, filename);
//        Path dumpPath = Paths.get(dumpsDirectoryPath.toString(), filename);
        return new File(dumpPath.toUri());
    }
}
