/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.file.viewer;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.util.LruCacheAdapter;

@UnstableApi
final class EncryptedMediaDataSource implements DataSource {

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

    EncryptedMediaDataSource(
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

    @Nullable
    @Override
    public Uri getUri() {
        return currentSpec != null ? currentSpec.uri : null;
    }

    @Override
    public void close() {
        isOpened = false;
        currentSpec = null;
        openPosition = 0;
        openRemaining = C.LENGTH_UNSET;
    }

    @Override
    public void addTransferListener(@NonNull TransferListener transferListener) {
        Log.d(TAG, "addTransferListener called");
    }

    private static final class BlockPosition {
        private final int blockIndex;
        private final int offsetInBlock;

        BlockPosition(int index, int offset) {
            blockIndex = index;
            offsetInBlock = offset;
        }
    }

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
