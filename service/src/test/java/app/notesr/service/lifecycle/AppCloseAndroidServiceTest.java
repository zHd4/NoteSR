/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.lifecycle;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Intent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppCloseAndroidServiceTest {

    private AppCloseAndroidService service;

    @BeforeEach
    void setUp() {
        service = spy(new AppCloseAndroidService());
    }

    @Test
    void testOnTaskRemovedWhenNoOtherServicesRunningClosesEverythingAndExits() {
        doReturn(0L).when(service).getOtherRunningServicesCount();
        doNothing().when(service).clearSecretCache();
        doNothing().when(service).closeDatabase();
        doNothing().when(service).destroySecrets();
        doNothing().when(service).stopForegroundService();
        doNothing().when(service).stopService();
        doNothing().when(service).callSuperOnTaskRemoved(any(Intent.class));
        doNothing().when(service).exitProcess();

        Intent intent = new Intent();

        service.onTaskRemoved(intent);

        verify(service, description("Secret cache should be cleared" +
                " when no other services are running"))
                .clearSecretCache();
        verify(service, description("Database should be closed" +
                " when no other services are running"))
                .closeDatabase();
        verify(service, description("Secrets should be destroyed" +
                " when no other services are running"))
                .destroySecrets();
        verify(service, description("Foreground notification should be stopped" +
                " when no other services are running"))
                .stopForegroundService();
        verify(service, description("Service should stop itself" +
                " when no other services are running"))
                .stopService();
        verify(service, description("Super.onTaskRemoved should be called" +
                " when no other services are running"))
                .callSuperOnTaskRemoved(intent);
        verify(service, description("Process should exit" +
                " when no other services are running"))
                .exitProcess();
    }

    @Test
    void testOnTaskRemovedWhenOtherServicesRunningOnlyStopsSelf() {
        doReturn(1L).when(service).getOtherRunningServicesCount();
        doNothing().when(service).stopForegroundService();
        doNothing().when(service).stopService();

        Intent intent = new Intent();

        service.onTaskRemoved(intent);

        verify(service, never().description("Secret cache should NOT be cleared" +
                " when other services are running"))
                .clearSecretCache();
        verify(service, never().description("Database should NOT be closed" +
                " when other services are running"))
                .closeDatabase();
        verify(service, never().description("Secrets should NOT be destroyed" +
                " when other services are running"))
                .destroySecrets();
        verify(service, never().description("Super.onTaskRemoved should NOT be called" +
                " when other services are running"))
                .callSuperOnTaskRemoved(any(Intent.class));
        verify(service, never().description("Process should NOT exit" +
                " when other services are running"))
                .exitProcess();
        verify(service, description("Foreground notification should be stopped" +
                " even if other services are running"))
                .stopForegroundService();
        verify(service, description("Service should stop itself" +
                " even if other services are running"))
                .stopService();
    }
}
