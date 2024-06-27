package com.peew.notesr.component;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.peew.notesr.App;
import com.peew.notesr.crypto.FilesCrypt;
import com.peew.notesr.db.notes.tables.DataBlocksTable;
import com.peew.notesr.db.notes.tables.FilesTable;
import com.peew.notesr.model.DataBlock;
import com.peew.notesr.model.EncryptedFileInfo;
import com.peew.notesr.model.FileInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.function.Consumer;

public class AssignmentsManager {
    private static final int CHUNK_SIZE = 500000;

    public void save(Long noteId, Uri fileUri) throws IOException {
        ContentResolver contentResolver = App.getContext().getContentResolver();
        Long fileId = saveInfo(noteId, fileUri);

        try (InputStream stream = contentResolver.openInputStream(fileUri)) {
            saveData(fileId, stream);
        } catch (IOException e) {
            Log.e("NoteSR", e.toString());
            throw new RuntimeException(e);
        }
    }

    public byte[] read(Long fileId) {
        FilesTable filesTable = getFilesTable();
        DataBlocksTable dataBlocksTable = getDataBlocksTable();

        Set<Long> ids = dataBlocksTable.getBlocksIdsByFileId(fileId);

        byte[] data = new byte[Math.toIntExact(filesTable.get(fileId).getSize())];
        int readBytes = 0;

        for (Long id : ids) {
            DataBlock dataBlock = dataBlocksTable.get(id);
            byte[] blockData = FilesCrypt.decryptData(dataBlock.getData());

            System.arraycopy(blockData, 0, data, readBytes, blockData.length);
            readBytes += blockData.length;
        }

        return data;
    }

    public long read(Long fileId, Consumer<byte[]> actionPerChunk) {
        DataBlocksTable dataBlocksTable = getDataBlocksTable();
        Set<Long> ids = dataBlocksTable.getBlocksIdsByFileId(fileId);

        long readBytes = 0;

        for (Long id : ids) {
            DataBlock dataBlock = dataBlocksTable.get(id);
            byte[] data = FilesCrypt.decryptData(dataBlock.getData());

            actionPerChunk.accept(data);
            readBytes += data.length;
        }

        return readBytes;
    }

    public void delete(Long fileId) {
        getDataBlocksTable().deleteByFileId(fileId);
    }

    private Long saveInfo(Long noteId, Uri fileUri) {
        String filename = getFileName(getCursor(fileUri));
        String type = getMimeType(filename);

        long size = getFileSize(getCursor(fileUri));

        FileInfo fileInfo = new FileInfo(noteId, size, filename, type);
        EncryptedFileInfo encryptedFileInfo = FilesCrypt.encryptInfo(fileInfo);

        getFilesTable().save(encryptedFileInfo);
        return encryptedFileInfo.getId();
    }

    private void saveData(Long fileId, InputStream stream) throws IOException {
        DataBlocksTable dataBlocksTable = getDataBlocksTable();

        byte[] chunk = new byte[CHUNK_SIZE];

        long order = 0;
        int bytesRead = stream.read(chunk);

        while (bytesRead != -1) {
            if (bytesRead != CHUNK_SIZE) {
                byte[] subChunk = new byte[bytesRead];
                System.arraycopy(chunk, 0, subChunk, 0, bytesRead);
                chunk = subChunk;
            }

            chunk = FilesCrypt.encryptData(chunk);
            dataBlocksTable.save(new DataBlock(fileId, order, chunk));

            chunk = new byte[CHUNK_SIZE];
            bytesRead = stream.read(chunk);

            order++;
        }
    }

    private Cursor getCursor(Uri uri) {
        Cursor cursor = App.getContext()
                .getContentResolver()
                .query(uri, null, null, null, null);

        if (cursor == null) {
            throw new RuntimeException(new NullPointerException("Cursor is null"));
        }

        return cursor;
    }

    private String getFileName(Cursor cursor) {
        try (cursor) {
            int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

            cursor.moveToFirst();
            return cursor.getString(index);
        }
    }

    private long getFileSize(Cursor cursor) {
        try (cursor) {
            int index = cursor.getColumnIndex(OpenableColumns.SIZE);

            cursor.moveToFirst();
            return cursor.getLong(index);
        }
    }

    private String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);

        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        return type;
    }

    private FilesTable getFilesTable() {
        return App.getAppContainer().getNotesDatabase().getTable(FilesTable.class);
    }

    private DataBlocksTable getDataBlocksTable() {
        return App.getAppContainer().getNotesDatabase().getTable(DataBlocksTable.class);
    }
}
