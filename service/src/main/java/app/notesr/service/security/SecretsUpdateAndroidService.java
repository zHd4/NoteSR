/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security;

import static java.util.Objects.requireNonNull;
import static app.notesr.core.util.CharUtils.bytesToChars;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

import app.notesr.core.security.SecretCache;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.security.dto.CryptoSecrets;

import app.notesr.core.util.FilesUtils;
import app.notesr.core.util.TransactionalFilesUtil;
import app.notesr.data.DatabaseProvider;
import app.notesr.service.AndroidService;
import app.notesr.service.AndroidServiceEntry;
import app.notesr.service.AndroidServiceRegistry;

public final class SecretsUpdateAndroidService extends AndroidService implements Runnable {

    public static final String NEW_KEY = "new_key";
    public static final String PASSWORD = "password";
    public static final String BROADCAST_ACTION = "re_encryption_service_broadcast";
    public static final String EXTRA_CURRENT_STATE = "current_state";
    public static final String EXTRA_COMPLETE = "re_encryption_complete";
    private static final String CHANNEL_ID = "re_encryption_service_channel";
    private static final String CHANNEL_NAME = "Re-encryption Service Channel";

    private String dbName;
    private CryptoManager cryptoManager;
    private SecretsUpdateStateHolder stateHolder;
    private CryptoSecrets newSecrets;
    private SecretsUpdateService secretsUpdateService;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        var notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_NONE);

        var notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(notificationChannel);

        var notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .build();

        int type = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
        }

        dbName = DatabaseProvider.DB_NAME;

        cryptoManager = CryptoManagerProvider.getInstance(getApplicationContext());
        newSecrets = getNewSecrets();

        var state = (SecretsUpdateState) intent.getSerializableExtra(EXTRA_CURRENT_STATE);
        stateHolder = new SecretsUpdateStateHolder(this::onStateUpdate).setState(state);
        secretsUpdateService = getSecretsUpdateService();

        var thread = new Thread(this);
        thread.start();

        startForeground(startId, notification, type);
        register();

        return START_STICKY;
    }

    @NonNull
    @Override
    protected AndroidServiceEntry getEntry() {
        return getEntry(null);
    }

    private AndroidServiceEntry getEntry(SecretsUpdateState state) {
        var payloadObj = new SecretsUpdateAndroidServiceStarter.Payload(
                newSecrets.getKey(),
                newSecrets.getPassword()
        );

        var encryptedPayload = getEncryptedJson(
                new ObjectMapper(),
                payloadObj,
                cryptoManager.getSecrets()
        );

        String stateJson = null;

        if (state != null) {
            stateJson = getPlainJson(new ObjectMapper(), state);
        }

        return entryBuilder(SecretsUpdateAndroidServiceStarter.class)
                .autoStart(true)
                .requiresAuth(true)
                .payload(encryptedPayload)
                .state(stateJson)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void run() {
        try {
            var filesUtils = new FilesUtils();
            var transactionId = stateHolder.getState().getTransactionId();
            var txFiles = new TransactionalFilesUtil(getApplicationContext(), filesUtils, transactionId);

            if (transactionId == null) {
                transactionId = txFiles.getTransactionId();
            }

            stateHolder.setState(stateHolder.getState().setTransactionId(transactionId));
            secretsUpdateService.updateSecrets(txFiles, cryptoManager, dbName, stateHolder,
                    newSecrets);

            onComplete();
        } catch (SecretsUpdateFailedException e) {
            // We need also to notify about the failure

            onDestroy();
            throw e;
        } finally {
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    private void onStateUpdate(SecretsUpdateState newState) {
        AndroidServiceRegistry.getInstance(getApplicationContext())
                .updateEntry(getEntry(newState));
    }

    private void onComplete() {
        var broadcastIntent = new Intent(BROADCAST_ACTION).putExtra(EXTRA_COMPLETE, true);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
    }

    private CryptoSecrets getNewSecrets() {
        try {
            byte[] newKey = SecretCache.take(NEW_KEY);
            char[] newPassword = bytesToChars(SecretCache.take(PASSWORD),
                    StandardCharsets.UTF_8);

            requireNonNull(newKey);
            requireNonNull(newPassword);

            return new CryptoSecrets(newKey, newPassword);
        } catch (CharacterCodingException e) {
            throw new RuntimeException(e);
        }
    }

    private SecretsUpdateService getSecretsUpdateService() {
        var context = getApplicationContext();
        var databaseManager = new DatabaseManagerImpl(context);

        return new SecretsUpdateService(context, databaseManager);
    }
}
