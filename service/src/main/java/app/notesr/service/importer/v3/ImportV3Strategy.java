/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.importer.v3;

import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;

import java.io.File;
import java.nio.file.Path;

import app.notesr.data.AppDatabase;
import app.notesr.service.file.FileService;
import app.notesr.service.importer.ImportStrategy;
import app.notesr.service.note.NoteService;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.dto.CryptoSecrets;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ImportV3Strategy implements ImportStrategy {

    private final CryptoSecrets cryptoSecrets;
    private final AppDatabase db;
    private final NoteService noteService;
    private final FileService fileService;
    private final File tempDecryptedBackupFile;

    @Override
    public void execute() {
        db.runInTransaction(() -> {
            BackupDecryptor decryptor = getBackupDecryptor();
            DataImporter dataImporter = getDataImporter(decryptor);
            dataImporter.importData();
            return null;
        });
    }

    private BackupDecryptor getBackupDecryptor() {
        AesGcmCryptor cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(cryptoSecrets));
        return new BackupDecryptor(cryptor);
    }

    DataImporter getDataImporter(BackupDecryptor decryptor) {
        Path backupZipPath = tempDecryptedBackupFile.toPath();
        return new DataImporter(decryptor, noteService, fileService, backupZipPath);
    }
}
