/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.migration.changes.security.crypto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import app.notesr.service.migration.AppMigrationException;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.service.security.AppSecurityException;
import app.notesr.service.security.AppSecurityService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class CbcToGcmKeyMigrationTest {

    @Mock
    private Context context;

    @Mock
    private AppSecurityService appSecurityService;

    private CbcToGcmKeyMigration cbcToGcmKeyMigration;

    @BeforeEach
    void setUp() {
        cbcToGcmKeyMigration = new CbcToGcmKeyMigration(1, 2) {
            @Override
            AppSecurityService getAppSecurityService(Context context) {
                return appSecurityService;
            }
        };
    }

    @Test
    void testMigrateSuccess() {
        CryptoSecrets secrets = new CryptoSecrets(new byte[]{1, 2, 3}, "123".toCharArray());

        when(appSecurityService.getActualSecrets()).thenReturn(secrets);
        doNothing().when(appSecurityService).setSecrets(secrets);

        assertDoesNotThrow(() -> cbcToGcmKeyMigration.migrate(context));
        verify(appSecurityService).getActualSecrets();
        verify(appSecurityService).setSecrets(secrets);
    }

    @Test
    void testMigrateWhenEncryptionFailsThrowsAppMigrationException() {
        doThrow(new AppSecurityException("Stub")).when(appSecurityService).setSecrets(any());

        AppMigrationException exception = assertThrows(
            AppMigrationException.class,
            () -> cbcToGcmKeyMigration.migrate(context)
        );

        assertEquals("Failed to migrate key", exception.getMessage());
    }

    @Test
    void testMigrateWhenSecretsValidationFailsThrowsAppMigrationException() {
        doThrow(new IllegalArgumentException()).when(appSecurityService).setSecrets(any());

        AppMigrationException exception = assertThrows(
                AppMigrationException.class,
                () -> cbcToGcmKeyMigration.migrate(context)
        );

        assertEquals("Failed to migrate key", exception.getMessage());
    }
}
