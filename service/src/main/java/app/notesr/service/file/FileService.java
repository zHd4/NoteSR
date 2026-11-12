/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.file;

import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.core.security.exception.EncryptionFailedException;
import app.notesr.core.util.FileExifDataResolver;
import app.notesr.core.util.FilesUtilsAdapter;
import app.notesr.core.util.HashUtils;
import app.notesr.core.util.thumbnail.ImageThumbnailCreator;
import app.notesr.core.util.thumbnail.ThumbnailCreator;
import app.notesr.core.util.thumbnail.VideoThumbnailCreator;
import app.notesr.data.AppDatabase;
import app.notesr.data.model.FileBlobInfo;
import app.notesr.data.model.FileInfo;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Manages note file metadata and encrypted file contents stored as ordered blobs.
 *
 * <p>File metadata is persisted through the database, while encrypted blob data
 * is stored in the application's internal {@value #BLOBS_DIR_NAME} directory.
 */
@RequiredArgsConstructor
public class FileService {

    private static final String TAG = FileService.class.getSimpleName();

    /** Name of the internal directory containing encrypted file blobs. */
    public static final String BLOBS_DIR_NAME = "fblobs";

    private static final int FILE_BLOB_MAX_SIZE = 500000;

    private final Context context;
    private final AppDatabase db;
    private final AesCryptor cryptor;
    private final FilesUtilsAdapter filesUtils;

    /**
     * Returns the number of files associated with a note.
     *
     * @param noteId identifier of the note
     * @return the number of files
     * @throws NullPointerException if the database returns a null count
     */
    public long getFilesCount(String noteId) {
        Long count = db.getFileInfoDao().getCountByNoteId(noteId);

        if (count == null) {
            throw new NullPointerException("Files count is null");
        }

        return count;
    }

    /**
     * Returns metadata for all files, with each file's decimal ID populated.
     *
     * @return metadata for all files
     */
    public List<FileInfo> getFilesInfo() {
        return db.getFileInfoDao()
                .getAll()
                .stream()
                .map(this::setDecimalId)
                .collect(Collectors.toList());
    }

    /**
     * Returns metadata for the files associated with a note, with decimal IDs populated.
     *
     * @param noteId identifier of the note
     * @return metadata for the note's files
     */
    public List<FileInfo> getFilesInfo(String noteId) {
        List<FileInfo> filesInfos = db.getFileInfoDao().getByNoteId(noteId);

        return filesInfos.stream()
                .map(this::setDecimalId)
                .collect(Collectors.toList());
    }

    /**
     * Returns metadata for all stored file blobs.
     *
     * @return metadata for all blobs
     */
    public List<FileBlobInfo> getFilesBlobInfo() {
        return db.getFileBlobInfoDao().getAll();
    }

    /**
     * Returns the blob identifiers associated with a file.
     *
     * @param fileId identifier of the file
     * @return identifiers of the file's blobs
     */
    public List<String> getFileBlobInfoIds(String fileId) {
        return db.getFileBlobInfoDao().getBlobIdsByFileId(fileId);
    }

    /**
     * Returns metadata for a file and populates its decimal ID.
     *
     * @param fileId identifier of the file
     * @return the file metadata
     */
    public FileInfo getFileInfo(String fileId) {
        return setDecimalId(db.getFileInfoDao().get(fileId));
    }

    /**
     * Returns metadata for a blob.
     *
     * @param blobInfoId identifier of the blob metadata record
     * @return the blob metadata
     */
    public FileBlobInfo getFileBlobInfo(String blobInfoId) {
        return db.getFileBlobInfoDao().get(blobInfoId);
    }

    /**
     * Creates metadata and encrypted blob data for each supplied content URI.
     *
     * <p>The database operations are run in a transaction. The supplied note ID
     * and URI list must not be null, and the URI list must not be empty.
     *
     * @param noteId identifier of the note to associate with the files
     * @param filesUri URIs of the file contents to save
     * @throws IllegalArgumentException if the note ID is null or the URI list is null or empty
     * @throws IOException if a file cannot be read or written
     * @throws DecryptionFailedException if the file content operation fails with a security error
     */
    public void saveFiles(String noteId, List<Uri> filesUri)
            throws IOException, DecryptionFailedException {

        if (noteId == null) {
            throw new IllegalArgumentException("Note id is null");
        }

        if (filesUri == null || filesUri.isEmpty()) {
            throw new IllegalArgumentException("Files doesn't provided");
        }

        try {
            db.runInTransaction(() -> filesUri.forEach(uri -> {
                try {
                    String fileId = saveFileInfo(getFileInfo(noteId, uri));
                    saveFileData(fileId, uri);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (DecryptionFailedException e) {
                    throw new IllegalStateException(e);
                }
            }));
        } catch (UncheckedIOException e) {
            throw requireNonNull(e.getCause());
        } catch (IllegalStateException e) {
            throw new DecryptionFailedException(e);
        }
    }

    /**
     * Inserts or updates file metadata and refreshes the parent note's update time.
     *
     * <p>A missing file ID is generated, and missing creation or update timestamps
     * are initialized to the current time.
     *
     * @param fileInfo metadata to persist
     * @return the persisted file identifier
     */
    public String saveFileInfo(FileInfo fileInfo) {
        return db.runInTransaction(() -> {
            if (fileInfo.getId() == null) {
                fileInfo.setId(randomUUID().toString());
            }

            if (fileInfo.getCreatedAt() == null) {
                fileInfo.setCreatedAt(LocalDateTime.now());
            }

            if (fileInfo.getUpdatedAt() == null) {
                fileInfo.setUpdatedAt(LocalDateTime.now());
            }

            if (db.getFileInfoDao().get(fileInfo.getId()) == null) {
                db.getFileInfoDao().insert(fileInfo);
            } else {
                db.getFileInfoDao().update(fileInfo);
            }

            db.getNoteDao().setUpdatedAtById(fileInfo.getNoteId(), LocalDateTime.now());

            return fileInfo.getId();
        });
    }

    /**
     * Opens the content URI and saves its contents as encrypted blobs.
     *
     * @param fileId identifier of the file whose contents are being saved
     * @param uri URI of the content to read
     * @throws IOException if the content cannot be read or stored
     * @throws DecryptionFailedException if a security error occurs while saving the content
     */
    public void saveFileData(String fileId, Uri uri)
            throws IOException, DecryptionFailedException {

        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        saveFileData(fileId, inputStream);
    }

    /**
     * Replaces a file's existing blob data with encrypted chunks from an input stream.
     *
     * <p>The input stream is closed after processing.
     *
     * @param fileId identifier of the file whose contents are being saved
     * @param inputStream stream containing the plaintext file data
     * @throws IOException if the stream or file storage cannot be read or written
     * @throws DecryptionFailedException if a security error occurs while storing the content
     */
    public void saveFileData(String fileId, InputStream inputStream)
            throws IOException, DecryptionFailedException {

        File blobsDir = filesUtils.getInternalFile(context, BLOBS_DIR_NAME);

        if (!blobsDir.exists()) {
            Files.createDirectories(blobsDir.toPath());
        }

        if (db.getFileInfoDao().get(fileId) != null) {
            List<String> blobsId = db.getFileBlobInfoDao().getBlobIdsByFileId(fileId);

            if (blobsId != null && !blobsId.isEmpty()) {
                for (String id : blobsId) {
                    File blobFile = new File(blobsDir, id);
                    Files.deleteIfExists(blobFile.toPath());
                }
            }
        }

        try {
            writeFileData(fileId, inputStream, blobsDir);
        } catch (GeneralSecurityException e) {
            throw new DecryptionFailedException(e);
        }
    }

    /**
     * Reads and decrypts all blobs for a file into a single byte array.
     *
     * @param fileId identifier of the file to read
     * @return the complete plaintext file contents
     * @throws IOException if a blob cannot be read
     * @throws DecryptionFailedException if a blob cannot be decrypted
     */
    public byte[] read(String fileId) throws IOException, DecryptionFailedException {
        Set<String> ids = new LinkedHashSet<>(db.getFileBlobInfoDao().getBlobIdsByFileId(fileId));
        File blobsDir = filesUtils.getInternalFile(context, BLOBS_DIR_NAME);

        Long fileSize = requireNonNull(db.getFileInfoDao().get(fileId)).getSize();
        byte[] fileBytes = new byte[Math.toIntExact(fileSize)];
        int readBytes = 0;

        for (String id : ids) {
            File blobFile = new File(blobsDir, id);

            try {
                byte[] blob = cryptor.decrypt(filesUtils.readFileBytes(blobFile));

                System.arraycopy(blob, 0, fileBytes, readBytes, blob.length);
                readBytes += blob.length;
            } catch (GeneralSecurityException e) {
                throw new DecryptionFailedException(e);
            }
        }

        return fileBytes;
    }

    /**
     * Reads and decrypts a file one blob at a time, passing each plaintext chunk to an action.
     *
     * @param fileId identifier of the file to read
     * @param actionPerChunk action to invoke with each decrypted blob
     * @return the total number of plaintext bytes passed to the action
     * @throws IOException if a blob cannot be read
     * @throws DecryptionFailedException if a blob cannot be decrypted
     */
    public long read(String fileId, Consumer<byte[]> actionPerChunk)
            throws IOException, DecryptionFailedException {

        Set<String> ids = new LinkedHashSet<>(db.getFileBlobInfoDao().getBlobIdsByFileId(fileId));
        File blobsDir = filesUtils.getInternalFile(context, BLOBS_DIR_NAME);

        long readBytes = 0;

        for (String id : ids) {
            File blobFile = new File(blobsDir, id);

            try {
                byte[] blob = cryptor.decrypt(filesUtils.readFileBytes(blobFile));

                actionPerChunk.accept(blob);
                readBytes += blob.length;
            } catch (GeneralSecurityException e) {
                throw new DecryptionFailedException(e);
            }
        }

        return readBytes;
    }

    /**
     * Deletes a file's blob data and metadata, then refreshes its parent note's update time.
     *
     * @param fileId identifier of the file to delete
     * @throws IOException if a blob file cannot be deleted
     */
    public void delete(String fileId) throws IOException {
        FileInfo fileInfo = db.getFileInfoDao().get(fileId);

        try {
            db.runInTransaction(() -> {
                File blobsDir = filesUtils.getInternalFile(context, BLOBS_DIR_NAME);

                for (String blobId : db.getFileBlobInfoDao().getBlobIdsByFileId(fileId)) {
                    File blobFile = new File(blobsDir, blobId);
                    try {
                        Files.deleteIfExists(blobFile.toPath());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }

                db.getFileBlobInfoDao().deleteByFileId(fileId);
                db.getFileInfoDao().delete(fileInfo);
                db.getNoteDao().setUpdatedAtById(fileInfo.getNoteId(), LocalDateTime.now());
            });
        } catch (UncheckedIOException e) {
            throw requireNonNull(e.getCause());
        }
    }

    /**
     * Imports a file metadata record without generating identifiers or timestamps.
     *
     * @param fileInfo metadata record to insert
     */
    public void importFileInfo(FileInfo fileInfo) {
        db.getFileInfoDao().insert(fileInfo);
    }

    /**
     * Imports blob metadata after verifying that its parent file exists.
     *
     * @param fileBlobInfo blob metadata record to insert
     * @throws IllegalArgumentException if the blob's parent file does not exist
     */
    public void importFileBlobInfo(FileBlobInfo fileBlobInfo) {
        String fileId = fileBlobInfo.getFileId();

        if (db.getFileInfoDao().get(fileId) == null) {
            throw new IllegalArgumentException("File not found");
        }

        db.getFileBlobInfoDao().insert(fileBlobInfo);
    }

    /**
     * Encrypts and writes imported plaintext blob data to internal storage.
     *
     * @param blobId identifier used as the stored blob filename
     * @param blobData plaintext blob contents
     * @throws IOException if the blob cannot be written
     * @throws EncryptionFailedException if the blob cannot be encrypted
     */
    public void importFileBlobData(String blobId, byte[] blobData)
            throws IOException, EncryptionFailedException {
        File blobsDir = filesUtils.getInternalFile(context, BLOBS_DIR_NAME);

        if (!blobsDir.exists()) {
            Files.createDirectories(blobsDir.toPath());
        }

        File blobFile = new File(blobsDir, blobId);

        try {
            byte[] encryptedBlob = cryptor.encrypt(blobData);
            filesUtils.writeFileBytes(blobFile, encryptedBlob);
        } catch (GeneralSecurityException e) {
            throw new EncryptionFailedException(e);
        }
    }

    /**
     * Returns the total number of file metadata records.
     *
     * @return the number of files
     */
    public long getFilesCount() {
        return db.getFileInfoDao().getRowsCount();
    }

    /**
     * Returns the total number of blob metadata records.
     *
     * @return the number of blobs
     */
    public long getBlobsCount() {
        return db.getFileBlobInfoDao().getRowsCount();
    }

    /**
     * Reads an input stream, encrypts it in bounded chunks, and stores each chunk with metadata.
     *
     * @param fileId identifier of the file to associate with the chunks
     * @param inputStream plaintext file data stream; closed by this method
     * @param blobsDir directory in which encrypted blob files are stored
     * @throws IOException if reading the stream or writing a blob fails
     * @throws GeneralSecurityException if encryption fails
     */
    private void writeFileData(String fileId, InputStream inputStream, File blobsDir)
            throws IOException, GeneralSecurityException {

        try (inputStream) {
            requireNonNull(inputStream, "Input stream is null");

            byte[] blob = new byte[FILE_BLOB_MAX_SIZE];

            long order = 0;
            int bytesRead = inputStream.read(blob);

            while (bytesRead != -1) {
                if (bytesRead != FILE_BLOB_MAX_SIZE) {
                    byte[] subBlob = new byte[bytesRead];
                    System.arraycopy(blob, 0, subBlob, 0, bytesRead);
                    blob = subBlob;
                }

                FileBlobInfo blobInfo = new FileBlobInfo();

                blobInfo.setId(randomUUID().toString());
                blobInfo.setFileId(fileId);
                blobInfo.setOrder(order);

                db.getFileBlobInfoDao().insert(blobInfo);

                File blobFile = new File(blobsDir, blobInfo.getId());
                byte[] encryptedBlob = cryptor.encrypt(blob);

                filesUtils.writeFileBytes(blobFile, encryptedBlob);

                blob = new byte[FILE_BLOB_MAX_SIZE];
                bytesRead = inputStream.read(blob);

                order++;
            }
        }
    }

    /**
     * Resolves file metadata and an optional image or video thumbnail from a content URI.
     *
     * @param noteId identifier of the note to associate with the file
     * @param uri URI of the file content
     * @return newly constructed file metadata
     */
    private FileInfo getFileInfo(String noteId, Uri uri) {
        FileExifDataResolver resolver = new FileExifDataResolver(context, filesUtils, uri);

        String filename = resolver.getFileName();
        String mimeType = resolver.getMimeType();

        long size = resolver.getFileSize();

        FileInfo fileInfo = new FileInfo();

        fileInfo.setNoteId(noteId);
        fileInfo.setSize(size);
        fileInfo.setName(filename);

        if (mimeType != null) {
            fileInfo.setType(mimeType);
            fileInfo.setThumbnail(getFileThumbnail(uri, mimeType));
        }

        return fileInfo;
    }

    /**
     * Reads and decrypts a stored blob.
     *
     * @param blobId identifier of the blob
     * @return the plaintext blob contents
     * @throws IOException if the blob cannot be read
     * @throws DecryptionFailedException if the blob cannot be decrypted
     */
    public byte[] getBlobData(String blobId) throws IOException, DecryptionFailedException {
        try {
            File blobsDir = filesUtils.getInternalFile(context, BLOBS_DIR_NAME);
            File blobFile = new File(blobsDir, blobId);

            return cryptor.decrypt(filesUtils.readFileBytes(blobFile));
        } catch (GeneralSecurityException e) {
            throw new DecryptionFailedException(e);
        }
    }

    /**
     * Computes and assigns the decimal representation of a file's UUID.
     *
     * @param fileInfo metadata whose decimal ID is to be populated
     * @return the same metadata object, updated with its decimal ID
     */
    private FileInfo setDecimalId(FileInfo fileInfo) {
        UUID uuid = UUID.fromString(fileInfo.getId());
        long hash = HashUtils.getUUIDHash(uuid);

        fileInfo.setDecimalId(hash);

        return fileInfo;
    }

    /**
     * Creates a thumbnail for supported image and video MIME types.
     *
     * @param uri URI of the source file
     * @param mimeType MIME type used to select a thumbnail creator
     * @return the generated thumbnail, or {@code null} for unsupported types or thumbnail errors
     */
    private byte[] getFileThumbnail(Uri uri, String mimeType) {
        try {
            String type = mimeType.split("/")[0];
            ThumbnailCreator creator;

            if (type.equals("image")) {
                creator = new ImageThumbnailCreator(context, filesUtils);
            } else if (type.equals("video")) {
                creator = new VideoThumbnailCreator(context);
            } else {
                return null;
            }

            return requireNonNull(creator).getThumbnail(uri);
        } catch (UnsupportedOperationException | IOException e) {
            Log.e(TAG, "Failed to get thumbnail for file", e);
            return null;
        }
    }
}
