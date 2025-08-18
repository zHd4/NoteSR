package app.notesr.security.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static app.notesr.util.HashUtils.toSha256Bytes;
import static app.notesr.util.HashUtils.toSha256String;

import android.content.SharedPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;

import app.notesr.security.dto.CryptoSecrets;
import app.notesr.util.FilesUtilsAdapter;
import app.notesr.util.WiperAdapter;

@ExtendWith(MockitoExtension.class)
class CryptoManagerTest {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom(new byte[]{1, 2, 3});

    @Mock
    private SharedPreferences prefs;

    @Mock
    private SharedPreferences.Editor editor;

    @Mock
    private FilesUtilsAdapter filesUtils;

    @Mock
    private WiperAdapter wiper;

    @Mock
    private CryptorFactory cryptorFactory;

    private CryptoManager cryptoManager;

    @BeforeEach
    void setUp() {
        cryptoManager = new CryptoManager(prefs, filesUtils, wiper, SECURE_RANDOM, cryptorFactory);
    }

    @Test
    void testGenerateSecretsCreatesKeyOfCorrectSize() {
        CryptoSecrets secrets = cryptoManager.generateSecrets("pass");
        assertEquals(CryptoManager.KEY_SIZE, secrets.getKey().length);
        assertEquals("pass", secrets.getPassword());
    }

    @Test
    void testisKeyExistsReturnsTrueWhenFileExists() {
        File mockFile = mock(File.class);
        when(filesUtils.getInternalFile(null, "key.encrypted")).thenReturn(mockFile);
        when(mockFile.exists()).thenReturn(true);

        assertTrue(cryptoManager.isKeyExists(null));
    }

    @Test
    void testBlockSetsPrefAndWipesFile() throws IOException {
        when(prefs.edit()).thenReturn(editor);
        when(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor);

        File mockFile = mock(File.class);
        when(filesUtils.getInternalFile(null, "key.encrypted")).thenReturn(mockFile);

        cryptoManager.block(null);

        verify(wiper).wipeFile(mockFile);
        verify(editor).putBoolean("is_blocked", true);
        verify(editor).apply();
    }

    @Test
    void testUnblockRemovesMarkerFile() throws IOException {
        when(prefs.edit()).thenReturn(editor);
        when(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor);

        File markerFile = mock(File.class);
        when(filesUtils.getInternalFile(null, ".blocked")).thenReturn(markerFile);
        when(markerFile.exists()).thenReturn(true);
        when(markerFile.delete()).thenReturn(true);

        cryptoManager.unblock(null);

        verify(editor).putBoolean("is_blocked", false);
        verify(editor).apply();
        verify(markerFile).delete();
    }

    @Test
    void testVerifyKeyReturnsTrueWhenHashesMatch() throws Exception {
        byte[] key = new byte[]{1, 2, 3};
        String hash = toSha256String(key);
        when(prefs.getString("key_hash", null)).thenReturn(hash);

        boolean result = cryptoManager.verifyKey(null, key);

        assertTrue(result);
    }

    @Test
    void testVerifyKeyReturnsFalseWhenHashesDoNotMatch() throws Exception {
        byte[] key = new byte[]{1, 2, 3};
        byte[] otherKey = new byte[]{4, 5, 6};
        byte[] hash = toSha256Bytes(otherKey);
        when(prefs.getString("key_hash", null)).thenReturn(toSha256String(hash));

        boolean result = cryptoManager.verifyKey(null, key);

        assertFalse(result);
    }
}
