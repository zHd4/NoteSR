package app.notesr.service.file;

import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;

import android.content.Context;
import android.net.Uri;

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

@RequiredArgsConstructor
public final class FileService {
    public static final String BLOBS_DIR_NAME = "fblobs";
    private static final int FILE_BLOB_MAX_SIZE = 500000;

    private final Context context;
    private final AppDatabase db;
    private final AesCryptor cryptor;
    private final FilesUtilsAdapter filesUtils;

    public long getFilesCount(String noteId) {
        Long count = db.getFileInfoDao().getCountByNoteId(noteId);

        if (count == null) {
            throw new NullPointerException("Files count is null");
        }

        return count;
    }

    public List<FileInfo> getFilesInfo() {
        return db.getFileInfoDao()
                .getAll()
                .stream()
                .map(this::setDecimalId)
                .collect(Collectors.toList());
    }

    public List<FileInfo> getFilesInfo(String noteId) {
        List<FileInfo> filesInfos = db.getFileInfoDao().getByNoteId(noteId);

        return filesInfos.stream()
                .map(this::setDecimalId)
                .collect(Collectors.toList());
    }

    public List<FileBlobInfo> getFilesBlobInfo() {
        return db.getFileBlobInfoDao().getAll();
    }

    public List<String> getFileBlobInfoIds(String fileId) {
        return db.getFileBlobInfoDao().getBlobIdsByFileId(fileId);
    }

    public FileInfo getFileInfo(String fileId) {
        return setDecimalId(db.getFileInfoDao().get(fileId));
    }

    public FileBlobInfo getFileBlobInfo(String blobInfoId) {
        return db.getFileBlobInfoDao().get(blobInfoId);
    }

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

    public void saveFileData(String fileId, Uri uri)
            throws IOException, DecryptionFailedException {

        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        saveFileData(fileId, inputStream);
    }

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

    public void setThumbnail(String fileId, byte[] thumbnail) {
        FileInfo fileInfo = db.getFileInfoDao().get(fileId);

        if (fileInfo == null) {
            throw new IllegalArgumentException("File not found");
        }

        fileInfo.setThumbnail(thumbnail);
        db.getFileInfoDao().update(fileInfo);
    }

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

    public void importFileInfo(FileInfo fileInfo) {
        db.getFileInfoDao().insert(fileInfo);
    }

    public void importFileBlobInfo(FileBlobInfo fileBlobInfo) {
        String fileId = fileBlobInfo.getFileId();

        if (db.getFileInfoDao().get(fileId) == null) {
            throw new IllegalArgumentException("File not found");
        }

        db.getFileBlobInfoDao().insert(fileBlobInfo);
    }

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

    public long getFilesCount() {
        return db.getFileInfoDao().getRowsCount();
    }

    public long getBlobsCount() {
        return db.getFileBlobInfoDao().getRowsCount();
    }

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

    public byte[] getBlobData(String blobId) throws IOException, DecryptionFailedException {
        try {
            File blobsDir = filesUtils.getInternalFile(context, BLOBS_DIR_NAME);
            File blobFile = new File(blobsDir, blobId);

            return cryptor.decrypt(filesUtils.readFileBytes(blobFile));
        } catch (GeneralSecurityException e) {
            throw new DecryptionFailedException(e);
        }
    }

    private FileInfo setDecimalId(FileInfo fileInfo) {
        UUID uuid = UUID.fromString(fileInfo.getId());
        long hash = HashUtils.getUUIDHash(uuid);

        fileInfo.setDecimalId(hash);

        return fileInfo;
    }

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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
