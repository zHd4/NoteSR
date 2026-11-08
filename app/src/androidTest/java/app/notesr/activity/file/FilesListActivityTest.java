/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static java.util.Objects.requireNonNull;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import app.notesr.R;
import app.notesr.activity.DialogFactory;
import app.notesr.activity.file.viewer.FileViewerActivityBase;
import app.notesr.activity.file.viewer.OpenUnknownFileActivity;
import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.data.model.FileInfo;
import app.notesr.data.model.Note;
import app.notesr.di.service.file.FileServiceModule;
import app.notesr.di.service.note.NoteServiceModule;
import app.notesr.di.service.security.AppSecurityServiceModule;
import app.notesr.di.util.ActivityUtilsModule;
import app.notesr.di.util.DialogFactoryModule;
import app.notesr.service.file.FileService;
import app.notesr.service.note.NoteService;
import app.notesr.service.security.AppSecurityService;
import app.notesr.util.ActivityUtils;
import app.notesr.util.DatabaseUtils;
import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.UninstallModules;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
@HiltAndroidTest
@UninstallModules({
        NoteServiceModule.class,
        FileServiceModule.class,
        DialogFactoryModule.class,
        AppSecurityServiceModule.class,
        ActivityUtilsModule.class
})
public class FilesListActivityTest {
    private static final String NOTE_ID = "test-note-id";
    private static final String NOTE_NAME = "Test note";
    private static final String FILE_ID = "test-file-id";
    private static final String FILE_NAME = "attachment.bin";
    private static final long FILE_DECIMAL_ID = 17L;
    private static final long UI_TIMEOUT_MILLIS = 5000;

    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @BindValue
    NoteService noteService = mock(NoteService.class);

    @BindValue
    FileService fileService = mock(FileService.class);

    @BindValue
    DialogFactory dialogFactory = mock(DialogFactory.class);

    @BindValue
    AppSecurityService appSecurityService = mock(AppSecurityService.class);

    @BindValue
    ActivityUtils activityUtils = mock(ActivityUtils.class);

    private Context context;

    @Before
    public void setUp() {
        hiltRule.inject();

        context = ApplicationProvider.getApplicationContext();

        Note note = new Note();
        note.setId(NOTE_ID);
        note.setName(NOTE_NAME);
        note.setText("");

        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(FILE_ID);
        fileInfo.setNoteId(NOTE_ID);
        fileInfo.setName(FILE_NAME);
        fileInfo.setSize(1024L);
        fileInfo.setType("application/octet-stream");
        fileInfo.setUpdatedAt(LocalDateTime.now());
        fileInfo.setDecimalId(FILE_DECIMAL_ID);

        AlertDialog progressDialog = mock(AlertDialog.class);
        when(appSecurityService.isAuthConfigured()).thenReturn(true);
        when(noteService.get(NOTE_ID)).thenReturn(note);
        when(fileService.getFilesCount(NOTE_ID)).thenReturn(1L);
        when(fileService.getFilesInfo(NOTE_ID)).thenReturn(List.of(fileInfo));
        when(fileService.getFileInfo(FILE_ID)).thenReturn(fileInfo);
        when(dialogFactory.getThemedProgressDialog(anyInt())).thenReturn(progressDialog);
    }

    @Test
    public void testEmptyListShowsEmptyStateAndNoteTitle() {
        when(fileService.getFilesCount(NOTE_ID)).thenReturn(0L);
        when(fileService.getFilesInfo(NOTE_ID)).thenReturn(List.of());

        try (ActivityScenario<FilesListActivity> scenario = launchActivity()) {
            waitForFilesList(scenario, 0);

            scenario.onActivity(activity -> {
                assertEquals(
                        "(0) Files of: " + NOTE_NAME,
                        getActionBarTitle(scenario));
                assertEquals(
                        View.VISIBLE,
                        activity.findViewById(R.id.missingFilesLabel).getVisibility());
                assertEquals(
                        View.INVISIBLE,
                        activity.findViewById(R.id.filesListView).getVisibility());
            });
        }
    }

    @Test
    public void testPopulatedListOpensFileAndRefreshesAfterModifiedResult() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor viewerMonitor = instrumentation.addMonitor(
                OpenUnknownFileActivity.class.getName(), null, false);

        try (ActivityScenario<FilesListActivity> scenario = launchActivity()) {
            waitForFilesList(scenario, 1);
            assertEquals(
                    "(1) Files of: " + NOTE_NAME,
                    getActionBarTitle(scenario));

            scenario.onActivity(activity -> {
                ListView filesListView = activity.findViewById(R.id.filesListView);
                View fileRow = filesListView.getChildAt(0);
                assertNotNull("File row should be rendered", fileRow);

                TextView fileName = fileRow.findViewById(R.id.fileNameTextView);
                assertEquals(FILE_NAME, fileName.getText().toString());

                assertTrue("File row should handle clicks", filesListView.performItemClick(
                        fileRow, 0, FILE_DECIMAL_ID));
            });

            Activity viewer = instrumentation.waitForMonitorWithTimeout(
                    viewerMonitor, UI_TIMEOUT_MILLIS);
            assertNotNull("Unknown-file viewer should open", viewer);

            FileInfo openedFile = (FileInfo) viewer.getIntent()
                    .getSerializableExtra(FileViewerActivityBase.EXTRA_FILE_INFO);
            assertNotNull("Viewer should receive the selected file", openedFile);
            assertEquals(FILE_NAME, openedFile.getName());

            viewer.setResult(Activity.RESULT_OK,
                    new Intent().putExtra(FileViewerActivityBase.EXTRA_FILE_MODIFIED, true));
            viewer.finish();

            waitForFilesList(scenario, 1);
            verify(fileService, timeout(5000).times(2)).getFilesInfo(NOTE_ID);
        } finally {
            instrumentation.removeMonitor(viewerMonitor);
        }
    }

    @Test
    public void testMissingNoteIdThrowsIllegalStateException() {
        Intent intent = new Intent(context, FilesListActivity.class);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> FilesListActivity.requireNoteId(intent));
        assertEquals("Note id didn't provided", exception.getMessage());
    }

    @Test
    public void testMissingNoteFailureIsCaptured() {
        when(noteService.get(NOTE_ID)).thenReturn(null);

        try (ActivityScenario<FilesListActivity> scenario = launchActivity()) {
            Throwable exception = awaitDatabaseOperations(scenario);
            assertNotNull("Missing-note failure should be captured", exception);
            assertEquals("Note with id " + NOTE_ID + " not found", exception.getMessage());
        }
    }

    @Test
    public void testInvalidSuccessfulFileResultFinishesActivityGracefully() {
        try (ActivityScenario<FilesListActivity> scenario = launchActivity()) {
            scenario.onActivity(activity -> {
                ActivityResultCallback<ActivityResult> callback =
                        activity.getAddFilesResultCallback();

                callback.onActivityResult(new ActivityResult(Activity.RESULT_OK, null));
                assertTrue("Activity should finish when file-result data is missing",
                        activity.isFinishing());
                verify(activityUtils).showToastMessage(
                        activity.getString(R.string.files_list_missing_file_data),
                        Toast.LENGTH_SHORT);
            });
        }
    }

    @Test
    public void testMissingNoteIdLaunchFinishesActivityGracefully() {
        Intent intent = new Intent(context, FilesListActivity.class);

        try (ActivityScenario<FilesListActivity> scenario = ActivityScenario.launch(intent)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals("Activity should finish immediately when note id is missing",
                    Lifecycle.State.DESTROYED, scenario.getState());

            verify(activityUtils).showToastMessage(
                    context.getString(R.string.files_list_missing_note_id),
                    Toast.LENGTH_SHORT);
        }
    }

    @Test
    public void testGetFileUrisHandlesClipDataAndSingleUri() {
        try (ActivityScenario<FilesListActivity> scenario = launchActivity()) {
            scenario.onActivity(activity -> {
                Intent clipIntent = new Intent();

                clipIntent.setClipData(ClipData.newRawUri("files",
                        Uri.parse("file:///path/to/file1.txt")));

                ClipData clipData = requireNonNull(clipIntent.getClipData());
                clipData.addItem(new ClipData.Item(Uri.parse("file:///path/to/file2.txt")));

                List<Uri> fromClipData = activity.getFileUris(clipIntent);
                assertEquals(2, fromClipData.size());

                Intent singleUriIntent = new Intent();
                singleUriIntent.setData(Uri.parse("file:///path/to/file3.txt"));

                List<Uri> fromSingleUri = activity.getFileUris(singleUriIntent);
                assertEquals(1, fromSingleUri.size());
                assertEquals(Uri.parse("file:///path/to/file3.txt"), fromSingleUri.get(0));
            });
        }
    }

    @Test
    public void testFileSaveFailureFinishesActivityGracefully()
            throws DecryptionFailedException, IOException {
        doThrow(new IOException("disk full")).when(fileService)
                .saveFiles(eq(NOTE_ID), isA(List.class));

        try (ActivityScenario<FilesListActivity> scenario = launchActivity()) {
            scenario.onActivity(activity -> {
                ActivityResultCallback<ActivityResult> callback =
                        activity.getAddFilesResultCallback();

                Intent data = new Intent();
                data.setData(Uri.parse("file:///path/to/file.txt"));

                callback.onActivityResult(new ActivityResult(Activity.RESULT_OK, data));
            });

            Throwable exception = awaitDatabaseOperations(scenario);
            assertNull("File-save failure should be handled without an uncaught exception",
                    exception);

            scenario.onActivity(activity ->
                    assertTrue("Activity should finish when saving files fails",
                            activity.isFinishing()));

            verify(activityUtils).showToastMessage(
                    context.getString(R.string.files_list_save_failed),
                    Toast.LENGTH_SHORT);
        }
    }

    @Test
    public void testFileModifiedViewerResultRefreshesFilesList() {
        try (ActivityScenario<FilesListActivity> scenario = launchActivity()) {
            waitForFilesList(scenario, 1);

            scenario.onActivity(activity -> {
                ActivityResultCallback<ActivityResult> callback =
                        activity.getViewerResultCallback();

                Intent data = new Intent();
                data.putExtra(FileViewerActivityBase.EXTRA_FILE_MODIFIED, true);

                callback.onActivityResult(new ActivityResult(Activity.RESULT_OK, data));
            });

            waitForFilesList(scenario, 1);
            verify(fileService, times(2)).getFilesInfo(NOTE_ID);
        }
    }

    @Test
    public void testHomeButtonAndDefaultMenuItemAreHandledAsExpected() {
        try (ActivityScenario<FilesListActivity> scenario = launchActivity()) {
            scenario.onActivity(activity -> {
                MenuItem home = mock(MenuItem.class);
                when(home.getItemId()).thenReturn(android.R.id.home);
                assertTrue(activity.onOptionsItemSelected(home));

                MenuItem other = mock(MenuItem.class);
                when(other.getItemId()).thenReturn(android.R.id.home + 1);
                assertFalse(activity.onOptionsItemSelected(other));
            });
        }
    }

    private ActivityScenario<FilesListActivity> launchActivity() {
        Intent intent = new Intent(context, FilesListActivity.class)
                .putExtra(FilesListActivity.EXTRA_NOTE_ID, NOTE_ID);

        return ActivityScenario.launch(intent);
    }

    private String getActionBarTitle(ActivityScenario<FilesListActivity> scenario) {
        AtomicReference<String> titleRef = new AtomicReference<>();

        scenario.onActivity(activity -> {
            ActionBar actionBar = requireNonNull(activity.getSupportActionBar());
            titleRef.set(requireNonNull(actionBar.getTitle()).toString());
        });

        return titleRef.get();
    }

    private void waitForFilesList(ActivityScenario<FilesListActivity> scenario, int count) {
        Throwable exception = awaitDatabaseOperations(scenario);
        if (exception != null) {
            throw new AssertionError("Uncaught exception in database operation",
                    exception);
        }

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        scenario.onActivity(activity -> {
            ListView filesListView = activity.findViewById(R.id.filesListView);

            assertNotNull("File list adapter should be initialized",
                    filesListView.getAdapter());
            assertEquals(count, filesListView.getAdapter().getCount());
            assertEquals(count == 0 ? View.INVISIBLE : View.VISIBLE, filesListView.getVisibility());
        });
    }

    private Throwable awaitDatabaseOperations(ActivityScenario<FilesListActivity> scenario) {
        AtomicReference<Throwable> exception = new AtomicReference<>();

        scenario.onActivity(activity ->
                exception.set(DatabaseUtils.awaitDatabaseOperations(activity.databaseExecutor,
                        activity.uncaughtDatabaseException)));

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        return exception.get();
    }

}
