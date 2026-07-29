/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security.rotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.TransactionalFilesUtil;
import app.notesr.service.AndroidServiceEntry;
import app.notesr.service.AndroidServiceRegistry;
import app.notesr.service.security.AppSecurityService;

@ExtendWith(MockitoExtension.class)
class SecretsRotationAndroidServiceTest {

    @Mock
    private Context context;

    @Mock
    private Intent intent;

    @Mock
    private AppSecurityService appSecurityService;

    @Mock
    private SecretsRotationService secretsRotationService;

    @Mock
    private TransactionalFilesUtil txFiles;

    @Mock
    private LocalBroadcastManager localBroadcastManager;

    @Mock
    private AndroidServiceRegistry androidServiceRegistry;

    private SecretsRotationAndroidService service;
    private SecretsRotationState state;
    private CryptoSecrets newSecrets;

    @BeforeEach
    void setUp() {
        service = spy(new SecretsRotationAndroidService());
        state = new SecretsRotationState();
        newSecrets = new CryptoSecrets(new byte[32], "password".toCharArray());

        // Inject basic dependencies using setters
        service.setAppSecurityService(appSecurityService);
        service.setNewSecrets(newSecrets);
        service.setSecretsRotationService(secretsRotationService);
        service.setDbName("test.db");
    }

    @Test
    void testRunSuccess() {
        service.setStateHolder(new SecretsRotationStateHolder(s -> {})
                .setState(state));

        doReturn(txFiles).when(service).getTransactionalFilesUtil(any());
        doNothing().when(service).onComplete();
        doNothing().when(service).stopService();
        
        when(txFiles.getTransactionId()).thenReturn("tx-123");

        service.run();

        verify(secretsRotationService)
                .updateSecrets(eq(txFiles), eq(appSecurityService), eq("test.db"), any(),
                        eq(newSecrets));
        verify(service).onComplete();
        verify(service).stopService();
    }

    @Test
    void testRunFailure() {
        service.setStateHolder(new SecretsRotationStateHolder(s -> {})
                .setState(state));

        MockedStatic<Log> logMock = mockStatic(Log.class);

        try (logMock) {
            doReturn(txFiles).when(service).getTransactionalFilesUtil(any());
            doNothing().when(service).onFail();
            doNothing().when(service).stopService();
            
            when(txFiles.getTransactionId()).thenReturn("tx-123");
            doThrow(new SecretsRotationFailedException("Failed"))
                    .when(secretsRotationService).updateSecrets(any(), any(), any(), any(), any());

            service.run();

            verify(service).onFail();
            verify(service).stopService();
        }
    }

    @Test
    void testOnCompleteCallsSendBroadcast() {
        doNothing().when(service).sendUpdateBroadcast(anyString());
        service.onComplete();
        verify(service).sendUpdateBroadcast(SecretsRotationAndroidService.EXTRA_COMPLETE);
    }

    @Test
    void testOnFailCallsSendBroadcast() {
        doNothing().when(service).sendUpdateBroadcast(anyString());
        service.onFail();
        verify(service).sendUpdateBroadcast(SecretsRotationAndroidService.EXTRA_FAIL);
    }

    @Test
    void testSendUpdateBroadcast() {
        MockedStatic<LocalBroadcastManager> mockedStatic = mockStatic(LocalBroadcastManager.class);

        try (mockedStatic) {
            mockedStatic.when(() -> LocalBroadcastManager.getInstance(any()))
                    .thenReturn(localBroadcastManager);

            doReturn(context).when(service).getApplicationContext();

            MockedConstruction<Intent> mockedIntent = mockConstruction(Intent.class,
                    (mock, context) ->
                            doReturn(mock).when(mock).putExtra(anyString(), anyBoolean()));

            try (mockedIntent) {
                service.sendUpdateBroadcast(SecretsRotationAndroidService.EXTRA_COMPLETE);

                verify(localBroadcastManager).sendBroadcast(any(Intent.class));
            }
        }
    }

    @Test
    void testOnStateUpdateUpdatesRegistry() {
        MockedStatic<AndroidServiceRegistry> mockedStatic = mockStatic(AndroidServiceRegistry.class);

        try (mockedStatic) {
            mockedStatic.when(() -> AndroidServiceRegistry.getInstance(any()))
                    .thenReturn(androidServiceRegistry);

            doReturn(context).when(service).getApplicationContext();

            // Inject encryptedPayload into the service since it's used in onStateUpdate
            service.setEncryptedPayload("encryptedPayload");
            doReturn("serializedState").when(service).serializeState(any());

            service.onStateUpdate(state);

            ArgumentCaptor<AndroidServiceEntry> captor =
                    ArgumentCaptor.forClass(AndroidServiceEntry.class);
            verify(androidServiceRegistry).updateEntry(captor.capture());

            AndroidServiceEntry entry = captor.getValue();
            assertEquals("encryptedPayload", entry.getPayload(), "Payload mismatch");
            assertEquals("serializedState", entry.getState(), "State mismatch");
            assertTrue(entry.isAutoStart(), "AutoStart should be true");
            assertTrue(entry.isRequiresAuth(), "RequiresAuth should be true");
        }
    }

    @Test
    void testOnStartCommand() {
        when(intent.getSerializableExtra(SecretsRotationAndroidService.EXTRA_CURRENT_STATE))
                .thenReturn(state);

        MockedStatic<AndroidServiceRegistry> registryMock = mockStatic(AndroidServiceRegistry.class);

        try (registryMock) {
            registryMock.when(() -> AndroidServiceRegistry.getInstance(any()))
                    .thenReturn(androidServiceRegistry);

            doReturn(context).when(service).getApplicationContext();
            doReturn(secretsRotationService).when(service).getSecretsRotationService();
            doReturn(newSecrets).when(service).getNewSecrets();
            doNothing().when(service).showForegroundNotification(anyInt());
            doReturn("encryptedPayload").when(service).encryptPayload(any());
            doReturn("serializedState").when(service).serializeState(any());

            service.onStartCommand(intent, 0, 1);

            verify(service).showForegroundNotification(1);

            ArgumentCaptor<AndroidServiceEntry> captor = ArgumentCaptor.forClass(AndroidServiceEntry.class);
            verify(androidServiceRegistry).register(captor.capture());

            AndroidServiceEntry entry = captor.getValue();
            assertEquals("encryptedPayload", entry.getPayload(), "Payload mismatch");
            assertEquals("serializedState", entry.getState(), "State mismatch");
        }
    }
}
