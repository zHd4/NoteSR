package app.notesr.activity.file.viewer;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;

import java.io.File;
import java.io.IOException;
import java.util.List;

import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.util.LruCacheAdapterImpl;

@UnstableApi
final class EncryptedMediaDataSourceFactory implements DataSource.Factory {

    private final AesCryptor cryptor;
    private final List<File> blockFiles;
    private final int blockMetadataLength;
    private final int cacheBlocks;

    EncryptedMediaDataSourceFactory(
            AesCryptor cryptor,
            List<File> blockFiles,
            int blockMetadataLength,
            int cacheBlocks) {

        this.cryptor = cryptor;
        this.blockFiles = blockFiles;
        this.blockMetadataLength = blockMetadataLength;
        this.cacheBlocks = Math.max(1, cacheBlocks);
    }

    @NonNull
    @Override
    public DataSource createDataSource() {
        try {
            return new EncryptedMediaDataSource(cryptor, blockFiles,
                    new LruCacheAdapterImpl(Math.max(1, cacheBlocks)), blockMetadataLength);

        } catch (IOException e) {
            return new DataSource() {
                @Override
                public void addTransferListener(@NonNull TransferListener transferListener) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public long open(@NonNull DataSpec dataSpec) throws IOException {
                    throw new IOException("Failed to create EncryptedMediaDataSource", e);
                }

                @Override
                public int read(@NonNull byte[] buffer, int offset, int readLength)
                        throws IOException {

                    throw new IOException("Not opened");
                }

                @Nullable
                @Override
                public Uri getUri() {
                    return null;
                }

                @Override
                public void close() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
