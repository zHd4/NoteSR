package app.notesr.activity.migration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import app.notesr.BuildConfig;
import app.notesr.service.migration.AppMigrationAndroidService;

public class MigrationBroadcastReceiverTest {

    @Test
    public void testOnReceiveWithCompletedMigrationInvokesCallback() {
        AtomicBoolean callbackCalled = new AtomicBoolean(false);
        Runnable callback = () -> callbackCalled.set(true);

        MigrationBroadcastReceiver receiver = new MigrationBroadcastReceiver(callback);

        Intent intent = new Intent(AppMigrationAndroidService.BROADCAST_ACTION);

        intent.putExtra(AppMigrationAndroidService.EXTRA_CURRENT_DATA_SCHEMA_VERSION,
                BuildConfig.DATA_SCHEMA_VERSION);
        intent.putExtra(AppMigrationAndroidService.EXTRA_COMPLETE, true);

        receiver.onReceive(null, intent);
        assertTrue("Callback should be called when migration is complete",
                callbackCalled.get());
    }

    @Test
    public void testOnReceiveWithIncompleteMigrationDoesNotInvokeCallback() {
        AtomicBoolean callbackCalled = new AtomicBoolean(false);
        Runnable callback = () -> callbackCalled.set(true);

        MigrationBroadcastReceiver receiver = new MigrationBroadcastReceiver(callback);

        Intent intent = new Intent(AppMigrationAndroidService.BROADCAST_ACTION);

        intent.putExtra(AppMigrationAndroidService.EXTRA_CURRENT_DATA_SCHEMA_VERSION,
                BuildConfig.DATA_SCHEMA_VERSION);
        intent.putExtra(AppMigrationAndroidService.EXTRA_COMPLETE, false);

        receiver.onReceive(null, intent);
        assertFalse("Callback should not be called when migration is not complete",
                callbackCalled.get());
    }

    @Test
    public void testOnReceiveWithWrongActionDoesNotInvokeCallback() {
        AtomicBoolean callbackCalled = new AtomicBoolean(false);
        Runnable callback = () -> callbackCalled.set(true);

        MigrationBroadcastReceiver receiver = new MigrationBroadcastReceiver(callback);

        Intent intent = new Intent("some.other.ACTION");
        intent.putExtra(AppMigrationAndroidService.EXTRA_COMPLETE, true);

        receiver.onReceive(null, intent);
        assertFalse("Callback should not be called for wrong action",
                callbackCalled.get());
    }
}
