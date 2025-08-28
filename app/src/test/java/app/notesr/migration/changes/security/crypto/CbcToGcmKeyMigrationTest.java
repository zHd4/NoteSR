package app.notesr.migration.changes.security.crypto;

import android.content.Context;
import app.notesr.exception.EncryptionFailedException;
import app.notesr.migration.service.AppMigrationException;
import app.notesr.security.crypto.CryptoManager;
import app.notesr.security.dto.CryptoSecrets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CbcToGcmKeyMigrationTest {

    @Mock
    private Context context;

    @Mock
    private CryptoManager cryptoManager;

    private CbcToGcmKeyMigration migration;

    @BeforeEach
    void setUp() {
        migration = new CbcToGcmKeyMigration(1, 2) {
            @Override
            CryptoManager getCryptoManager() {
                return cryptoManager;
            }
        };
    }

    @Test
    void testMigrateSuccess() throws EncryptionFailedException {
        CryptoSecrets secrets = new CryptoSecrets(new byte[]{1, 2, 3}, "123");

        when(cryptoManager.getSecrets()).thenReturn(secrets);
        doNothing().when(cryptoManager).setSecrets(context, secrets);

        assertDoesNotThrow(() -> migration.migrate(context));
        verify(cryptoManager).getSecrets();
        verify(cryptoManager).setSecrets(context, secrets);
    }

    @Test
    void testMigrateWhenEncryptionFailsThrowsAppMigrationException()
            throws EncryptionFailedException {
        doThrow(new EncryptionFailedException()).when(cryptoManager).setSecrets(any(), any());

        AppMigrationException exception = assertThrows(
            AppMigrationException.class,
            () -> migration.migrate(context)
        );

        assertEquals("Failed to migrate key", exception.getMessage());
    }
}
