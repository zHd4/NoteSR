/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.lifecycle;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Intent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import app.notesr.service.security.AppSecurityService;

@ExtendWith(MockitoExtension.class)
class AppCloseAndroidServiceTest {

    private AppSecurityService appSecurityService;
    private AppCloseAndroidService appCloseAndroidService;

    @BeforeEach
    void setUp() {
        appSecurityService = mock(AppSecurityService.class);
        appCloseAndroidService = spy(new AppCloseAndroidService(appSecurityService));
    }

    @Test
    void testOnTaskRemovedWhenNoOtherServicesRunningClosesEverythingAndExits() {
        doReturn(0L).when(appCloseAndroidService).getOtherRunningServicesCount();
        doNothing().when(appSecurityService).logout();
        doNothing().when(appCloseAndroidService).stopForegroundService();
        doNothing().when(appCloseAndroidService).stopService();
        doNothing().when(appCloseAndroidService).callSuperOnTaskRemoved(any(Intent.class));
        doNothing().when(appCloseAndroidService).exitProcess();

        Intent intent = new Intent();

        appCloseAndroidService.onTaskRemoved(intent);

        verify(appSecurityService, description("User should be logged out" +
                " when no other services are running"))
                .logout();
        verify(appCloseAndroidService, description("Foreground notification should be stopped" +
                " when no other services are running"))
                .stopForegroundService();
        verify(appCloseAndroidService, description("Service should stop itself" +
                " when no other services are running"))
                .stopService();
        verify(appCloseAndroidService, description("Super.onTaskRemoved should be called" +
                " when no other services are running"))
                .callSuperOnTaskRemoved(intent);
        verify(appCloseAndroidService, description("Process should exit" +
                " when no other services are running"))
                .exitProcess();
    }

    @Test
    void testOnTaskRemovedWhenOtherServicesRunningOnlyStopsSelf() {
        doReturn(1L).when(appCloseAndroidService).getOtherRunningServicesCount();
        doNothing().when(appCloseAndroidService).stopForegroundService();
        doNothing().when(appCloseAndroidService).stopService();

        Intent intent = new Intent();

        appCloseAndroidService.onTaskRemoved(intent);

        verify(appSecurityService, never().description("User should NOT be logged out" +
                " when other services are running"))
                .logout();
        verify(appCloseAndroidService, never().description("Super.onTaskRemoved should NOT be called" +
                " when other services are running"))
                .callSuperOnTaskRemoved(any(Intent.class));
        verify(appCloseAndroidService, never().description("Process should NOT exit" +
                " when other services are running"))
                .exitProcess();
        verify(appCloseAndroidService, description("Foreground notification should be stopped" +
                " even if other services are running"))
                .stopForegroundService();
        verify(appCloseAndroidService, description("Service should stop itself" +
                " even if other services are running"))
                .stopService();
    }
}
