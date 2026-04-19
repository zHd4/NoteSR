/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.security.crypto;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.TransferListener;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import app.notesr.core.util.LruCacheAdapter;

/**
 * A {@link DataSource} that provides access to media data stored in encrypted blocks.
 * <p>
 * This implementation handles media files that have been split into multiple encrypted files (blocks).
 * It uses an {@link AesCryptor} to decrypt blocks on demand and maintains an {@link LruCacheAdapter}
 * to cache decrypted blocks for improved performance during playback and seeking.
 */
@OptIn(markerClass = UnstableApi.class)
public final class EncryptedMediaDataSource implements DataSource {

    private static final String TAG = EncryptedMediaDataSource.class.getCanonicalName();

    private final AesCryptor cryptor;
    private final List<File> blockFiles;
    private final long[] prefixSums;
    private final long totalPlainSize;
    private final LruCacheAdapter blockCache;

    private DataSpec currentSpec;
    private boolean isOpened = false;
    private long openPosition = 0;
    private long openRemaining = C.LENGTH_UNSET;

    /**
     * Constructs a new {@code EncryptedMediaDataSource}.
     *
     * @param cryptor             The {@link AesCryptor} used for decryption.
     * @param blockFiles          A list of {@link File} objects representing the encrypted blocks, in order.
     * @param lruCache            An {@link LruCacheAdapter} for caching decrypted block data.
     * @param blockMetadataLength The length of metadata (e.g., IV) at the beginning of each encrypted block file.
     * @throws IOException If an error occurs while calculating block sizes or if a block file is too small.
     */
    public EncryptedMediaDataSource(
            AesCryptor cryptor,
            List<File> blockFiles,
            LruCacheAdapter lruCache,
            int blockMetadataLength) throws IOException {

        if (cryptor == null) {
            throw new IllegalArgumentException("AesCryptor is null");
        }

        if (blockFiles == null || blockFiles.isEmpty()) {
            throw new IllegalArgumentException("blockFiles are null or empty");
        }

        this.cryptor = cryptor;
        this.blockFiles = blockFiles;
        this.blockCache = lruCache;

        int blockFilesCount = blockFiles.size();
        this.prefixSums = new long[blockFilesCount];
        long accumulatedSize = 0L;

        for (int i = 0; i < blockFilesCount; i++) {
            File blockFile = blockFiles.get(i);
            long blockFileLength = blockFile.length();

            if (blockFileLength < blockMetadataLength) {
                throw new IOException("Invalid block file (too small): " + blockFile.getName());
            }

            long plainLength = blockFileLength - blockMetadataLength;

            accumulatedSize += plainLength;
            prefixSums[i] = accumulatedSize;
        }

        this.totalPlainSize = accumulatedSize;
    }

    /**
     * Opens the data source to read the specified {@link DataSpec}.
     *
     * @param dataSpec The {@link DataSpec} defining the data to be read.
     * @return The number of bytes that can be read from the opened source.
     * @throws IOException If an error occurs opening the data source or if the start position is invalid.
     */
    @Override
    public long open(@NonNull DataSpec dataSpec) throws IOException {
        if (dataSpec == null) {
            throw new IOException("DataSpec is null");
        }

        long position = dataSpec.position;
        long length = dataSpec.length;

        if (position < 0 || position > totalPlainSize) {
            throw new IOException("Invalid start position: " + position);
        }

        this.currentSpec = dataSpec;
        this.openPosition = position;

        this.openRemaining = length == C.LENGTH_UNSET
                ? totalPlainSize - position
                : Math.min(length, totalPlainSize - position);

        this.isOpened = true;

        return this.openRemaining;
    }

    /**
     * Reads up to {@code readLength} bytes of decrypted data into {@code buffer}
     * starting at {@code offset}.
     *
     * @param buffer     The buffer into which the read data should be stored.
     * @param offset     The start offset in {@code buffer} at which the data should be written.
     * @param readLength The maximum number of bytes to read.
     * @return The number of bytes read, or {@link C#RESULT_END_OF_INPUT}
     * if the end of the source has been reached.
     * @throws IOException If an error occurs during reading or decryption.
     */
    @Override
    public int read(@NonNull byte[] buffer, int offset, int readLength) throws IOException {
        if (!isOpened) {
            throw new IOException("DataSource not opened");
        }

        if (readLength == 0) {
            return 0;
        }

        if (openRemaining == 0) {
            return C.RESULT_END_OF_INPUT;
        }

        int toRead = readLength;

        if (openRemaining != C.LENGTH_UNSET) {
            toRead = (int) Math.min(toRead, openRemaining);
        }

        int totalRead = 0;

        while (totalRead < toRead) {
            long absolutePosition = openPosition;
            BlockPosition blockPosition = locateBlock(absolutePosition);

            int blockIndex = blockPosition.blockIndex;
            int offsetInBlock = blockPosition.offsetInBlock;

            byte[] plainBlock = getDecryptedBlock(blockIndex);

            int copyLength = Math.min(toRead - totalRead, plainBlock.length - offsetInBlock);
            System.arraycopy(plainBlock, offsetInBlock, buffer, offset + totalRead, copyLength);

            totalRead += copyLength;
            openPosition += copyLength;

            if (openRemaining != C.LENGTH_UNSET) {
                openRemaining -= copyLength;
            }
        }

        return totalRead;
    }

    /**
     * Returns the {@link Uri} from which data is being read, or {@code null} if the source is not open.
     *
     * @return The {@link Uri}, or {@code null}.
     */
    @Nullable
    @Override
    public Uri getUri() {
        return currentSpec != null ? currentSpec.uri : null;
    }

    /**
     * Closes the data source.
     */
    @Override
    public void close() {
        isOpened = false;
        currentSpec = null;
        openPosition = 0;
        openRemaining = C.LENGTH_UNSET;
    }

    /**
     * Adds a {@link TransferListener} to the data source.
     *
     * @param transferListener The listener to add.
     */
    @Override
    public void addTransferListener(@NonNull TransferListener transferListener) {
        Log.d(TAG, "addTransferListener called");
    }

    /**
     * Represents a position within a specific block.
     */
    private static final class BlockPosition {
        /** The index of the block in the {@code blockFiles} list. */
        private final int blockIndex;
        /** The byte offset within the decrypted block. */
        private final int offsetInBlock;

        /**
         * Constructs a {@code BlockPosition}.
         *
         * @param index  The block index.
         * @param offset The offset in the block.
         */
        BlockPosition(int index, int offset) {
            blockIndex = index;
            offsetInBlock = offset;
        }
    }

    /**
     * Locates the block index and the offset within that block for a given absolute position.
     *
     * @param absolutePosition The absolute position in the plain media data.
     * @return A {@link BlockPosition} containing the block index and offset.
     * @throws IOException If the position is out of range.
     */
    private BlockPosition locateBlock(long absolutePosition) throws IOException {
        if (absolutePosition < 0 || absolutePosition >= totalPlainSize) {
            throw new IOException("Position out of range: " + absolutePosition);
        }

        int lowIndex = 0;
        int highIndex = prefixSums.length - 1;

        while (lowIndex < highIndex) {
            int middleIndex = (lowIndex + highIndex) >>> 1;

            if (absolutePosition < prefixSums[middleIndex]) {
                highIndex = middleIndex;
            } else {
                lowIndex = middleIndex + 1;
            }
        }

        int blockIndex = lowIndex;
        long previousSum = blockIndex != 0 ? prefixSums[blockIndex - 1] : 0;
        int offsetInBlock = (int) (absolutePosition - previousSum);

        return new BlockPosition(blockIndex, offsetInBlock);
    }

    /**
     * Retrieves the decrypted content of the block at the specified index.
     * <p>
     * This method first checks the cache. If not found, it reads the encrypted file,
     * decrypts it using {@link AesCryptor}, and caches the result.
     *
     * @param index The index of the block to retrieve.
     * @return The decrypted bytes of the block.
     * @throws IOException If an error occurs reading the file or during decryption.
     */
    private byte[] getDecryptedBlock(int index) throws IOException {
        synchronized (blockCache) {
            byte[] cached = blockCache.get(index);

            if (cached != null) {
                return cached;
            }
        }

        File blockFile = blockFiles.get(index);
        byte[] fileBytes = readFileFully(blockFile);

        try {
            byte[] plain = cryptor.decrypt(fileBytes);

            synchronized (blockCache) {
                blockCache.put(index, plain);
            }

            return plain;
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to decrypt block: " + blockFile.getName(), e);
        }
    }

    /**
     * Reads the entire contents of a file into a byte array.
     *
     * @param file The file to read.
     * @return The file contents as a byte array.
     * @throws IOException If an error occurs during reading.
     */
    private static byte[] readFileFully(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int readBytes;

            while ((readBytes = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, readBytes);
            }

            return outputStream.toByteArray();
        }
    }
}
