package app.notesr.migration.service;

import static org.junit.Assert.assertTrue;
import static app.notesr.migration.service.AppMigrationAndroidService.EXTRA_COMPLETE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class AppMigrationAndroidServiceTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void testMigrationBroadcastSent() throws Exception {
        IntentFilter filter = new IntentFilter(AppMigrationAndroidService.BROADCAST_ACTION);
        CountDownLatch latch = new CountDownLatch(1);

        boolean[] broadcastReceived = {false};

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                broadcastReceived[0] = intent.getBooleanExtra(EXTRA_COMPLETE, false);
                latch.countDown();
            }
        };

        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);

        Intent intent = new Intent(context, AppMigrationAndroidService.class);
        context.startService(intent);

        boolean await = latch.await(5, TimeUnit.SECONDS);

        assertTrue("Broadcast wasn't received", await);
        assertTrue("EXTRA_COMPLETE must be true", broadcastReceived[0]);

        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }
}