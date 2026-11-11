/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.note.list;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.SearchView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import app.notesr.R;
import app.notesr.activity.DialogFactory;
import app.notesr.activity.note.editor.OpenNoteActivity;
import app.notesr.activity.security.AuthActivity;
import app.notesr.data.model.Note;
import app.notesr.di.service.file.FileServiceModule;
import app.notesr.di.service.note.NoteServiceModule;
import app.notesr.di.service.security.AppSecurityServiceModule;
import app.notesr.di.util.DialogFactoryModule;
import app.notesr.service.file.FileService;
import app.notesr.service.note.NoteService;
import app.notesr.service.security.AppSecurityService;
import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.UninstallModules;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@HiltAndroidTest
@UninstallModules({
        NoteServiceModule.class,
        FileServiceModule.class,
        DialogFactoryModule.class,
        AppSecurityServiceModule.class
})
public class NotesListActivityTest {
    private static final String NOTE_ID = "test-note-id";
    private static final String NOTE_NAME = "Test note";
    private static final String NOTE_TEXT = "This is a test note";
    private static final long NOTE_DECIMAL_ID = 42L;
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

    private Context context;

    @Before
    public void setUp() {
        hiltRule.inject();
        context = ApplicationProvider.getApplicationContext();

        when(appSecurityService.isAuthConfigured()).thenReturn(true);

        AlertDialog progressDialog = mock(AlertDialog.class);
        when(dialogFactory.getThemedProgressDialog(anyInt())).thenReturn(progressDialog);
    }

    @Test
    public void testEmptyListShowsEmptyState() {
        when(noteService.getAll()).thenReturn(List.of());

        try (ActivityScenario<NotesListActivity> scenario = launchActivity()) {
            waitForNotesList(scenario, 0);

            scenario.onActivity(activity -> {
                assertEquals(View.VISIBLE,
                        activity.findViewById(R.id.missingNotesLabel).getVisibility());
                assertEquals(View.INVISIBLE,
                        activity.findViewById(R.id.notesListView).getVisibility());
            });
        }
    }

    @Test
    public void testPopulatedListShowsRenderedNoteContent() {
        when(noteService.getAll()).thenReturn(List.of(createTestNote()));

        try (ActivityScenario<NotesListActivity> scenario = launchActivity()) {
            waitForNotesList(scenario, 1);

            scenario.onActivity(activity -> {
                ListView notesListView = activity.findViewById(R.id.notesListView);
                View row = notesListView.getChildAt(0);
                assertNotNull("Note row should be rendered", row);

                TextView name = row.findViewById(R.id.noteNameTextView);
                TextView text = row.findViewById(R.id.noteTextView);

                assertEquals(NOTE_NAME, name.getText().toString());
                assertEquals(NOTE_TEXT, text.getText().toString());
            });
        }
    }

    @Test
    public void testExistingNoteOpensEditor() {
        Note note = createTestNote();
        when(noteService.getAll()).thenReturn(List.of(note));

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor editorActivityMonitor = instrumentation.addMonitor(
                OpenNoteActivity.class.getName(), null, false);

        try (ActivityScenario<NotesListActivity> scenario = launchActivity()) {
            waitForNotesList(scenario, 1);

            scenario.onActivity(activity -> {
                ListView notesListView = activity.findViewById(R.id.notesListView);
                View row = notesListView.getChildAt(0);

                assertNotNull("List row should be rendered", row);
                notesListView.performItemClick(row, 0, NOTE_DECIMAL_ID);
            });

            Activity opened = instrumentation.waitForMonitorWithTimeout(editorActivityMonitor,
                    UI_TIMEOUT_MILLIS);

            assertNotNull("Existing note should open in editor", opened);
            assertEquals(NOTE_ID,
                    opened.getIntent().getStringExtra(OpenNoteActivity.EXTRA_NOTE_ID));
        } finally {
            instrumentation.removeMonitor(editorActivityMonitor);
        }
    }

    @Test
    public void testCreateNewNoteStartsEditorWithoutNoteId() {
        Note note = createTestNote();
        when(noteService.getAll()).thenReturn(List.of(note));

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor editorActivityMonitor = instrumentation.addMonitor(
                OpenNoteActivity.class.getName(), null, false);

        try (ActivityScenario<NotesListActivity> scenario = launchActivity()) {
            waitForNotesList(scenario, 1);

            scenario.onActivity(activity -> {
                View addButton = activity.findViewById(R.id.addNoteButton);
                assertNotNull(addButton);
                addButton.performClick();
            });

            Activity opened = instrumentation.waitForMonitorWithTimeout(editorActivityMonitor,
                    UI_TIMEOUT_MILLIS);

            assertNotNull("New note should open in editor", opened);
            assertNull("New note intent should not include a note id",
                    opened.getIntent().getStringExtra(OpenNoteActivity.EXTRA_NOTE_ID));
        } finally {
            instrumentation.removeMonitor(editorActivityMonitor);
        }
    }

    @Test
    public void testSearchFiltering() {
        when(noteService.getAll()).thenReturn(List.of(createTestNote()));
        when(noteService.search("test")).thenReturn(List.of(createTestNote()));

        try (ActivityScenario<NotesListActivity> scenario = launchActivity()) {
            waitForNotesList(scenario, 1);

            scenario.onActivity(activity -> {
                Menu menu = new MenuBuilder(activity);
                activity.onCreateOptionsMenu(menu);

                MenuItem item = menu.findItem(R.id.searchNotesButton);
                assertNotNull(item);

                SearchView searchView = (SearchView) item.getActionView();
                assertNotNull(searchView);
                searchView.setQuery("test", true);
            });

            verify(noteService, timeout(5000).atLeastOnce()).search("test");
        }
    }

    @Test
    public void testSearchChangesAndClearsFilterForReload() {
        when(noteService.getAll()).thenReturn(List.of(createTestNote()));

        try (ActivityScenario<NotesListActivity> scenario = launchActivity()) {
            waitForNotesList(scenario, 1);

            scenario.onActivity(activity -> {
                when(noteService.search("test")).thenReturn(List.of(createTestNote()));

                SearchView.OnQueryTextListener queryListener = activity.getSearchQueryListener();
                assertTrue(queryListener.onQueryTextChange("test"));
            });

            SystemClock.sleep(500L);
            verify(noteService, timeout(5000).atLeastOnce()).search("test");

            scenario.onActivity(activity -> {
                when(noteService.getAll()).thenReturn(List.of(createTestNote()));

                SearchView.OnQueryTextListener queryListener = activity.getSearchQueryListener();
                assertTrue(queryListener.onQueryTextChange(""));
            });

            SystemClock.sleep(500L);
            verify(noteService, timeout(5000).atLeastOnce()).getAll();

            scenario.onActivity(activity -> {
                when(noteService.getAll()).thenReturn(List.of(createTestNote()));

                View.OnAttachStateChangeListener attachListener =
                        activity.getSearchViewAttachStateChangeListener();
                SearchView searchView = new SearchView(activity);

                attachListener.onViewDetachedFromWindow(searchView);
            });

            verify(noteService, timeout(5000).atLeastOnce()).getAll();
        }
    }

    @Test
    public void testLifecycleGuardDetectsFinishingState() {
        when(noteService.getAll()).thenReturn(List.of(createTestNote()));

        try (ActivityScenario<NotesListActivity> scenario = launchActivity()) {
            waitForNotesList(scenario, 1);

            scenario.onActivity(activity -> {
                assertFalse(activity.isFinishingOrDestroyed());
                activity.finish();
                assertTrue(activity.isFinishingOrDestroyed());
            });
        }
    }

    @Test
    public void testQueryTextChangeNormalizesNullQueryToReloadAllNotes() {
        when(noteService.getAll()).thenReturn(List.of(createTestNote()));

        try (ActivityScenario<NotesListActivity> scenario = launchActivity()) {
            waitForNotesList(scenario, 1);

            scenario.onActivity(activity -> {
                when(noteService.getAll()).thenReturn(List.of(createTestNote()));

                SearchView.OnQueryTextListener queryListener = activity.getSearchQueryListener();
                assertTrue(queryListener.onQueryTextChange(null));
            });

            SystemClock.sleep(500L);
            verify(noteService, timeout(5000).atLeastOnce()).getAll();
        }
    }

    @Test
    public void testEditorResultRefreshesOnlyForOkResult() {
        when(noteService.getAll()).thenReturn(List.of(createTestNote()));

        try (ActivityScenario<NotesListActivity> scenario = launchActivity()) {
            waitForNotesList(scenario, 1);

            scenario.onActivity(activity -> {
                ActivityResultCallback<ActivityResult> resultCallback =
                        activity.getOpenNoteResultCallback();

                reset(noteService);
                when(noteService.getAll()).thenReturn(List.of(createTestNote()));
                resultCallback.onActivityResult(new ActivityResult(Activity.RESULT_OK, null));
                verify(noteService, timeout(5000).atLeastOnce()).getAll();

                reset(noteService);
                resultCallback.onActivityResult(new ActivityResult(Activity.RESULT_CANCELED, null));
                verify(noteService, timeout(1000).times(0)).getAll();
            });
        }
    }

    @Test
    public void testSearchAndUnknownMenuItemsFallbackToSuper() {
        when(noteService.getAll()).thenReturn(List.of(createTestNote()));

        try (ActivityScenario<NotesListActivity> scenario = launchActivity()) {
            waitForNotesList(scenario, 1);

            scenario.onActivity(activity -> {
                MenuItem searchItem = mock(MenuItem.class);
                when(searchItem.getItemId()).thenReturn(R.id.searchNotesButton);
                assertFalse(activity.onOptionsItemSelected(searchItem));

                MenuItem unrecognizedItem = mock(MenuItem.class);
                when(unrecognizedItem.getItemId()).thenReturn(-1);
                assertFalse(activity.onOptionsItemSelected(unrecognizedItem));
            });
        }
    }

    @Test
    public void testOptionsMenuLockActionLogsOutAndStartsAuth() {
        when(noteService.getAll()).thenReturn(List.of(createTestNote()));

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor authActivityMonitor = instrumentation.addMonitor(
                AuthActivity.class.getName(), null, false);

        try (ActivityScenario<NotesListActivity> scenario = launchActivity()) {
            waitForNotesList(scenario, 1);

            scenario.onActivity(activity -> {
                MenuItem item = mock(MenuItem.class);
                when(item.getItemId()).thenReturn(R.id.lockAppButton);
                assertTrue(activity.onOptionsItemSelected(item));
            });

            Activity authActivity = instrumentation.waitForMonitorWithTimeout(authActivityMonitor,
                    UI_TIMEOUT_MILLIS);

            assertNotNull("Auth activity should start after locking", authActivity);
            verify(appSecurityService, timeout(5000)).logout();
        } finally {
            instrumentation.removeMonitor(authActivityMonitor);
        }
    }

    @Test
    public void testSystemBackLocksApp() {
        when(noteService.getAll()).thenReturn(List.of(createTestNote()));

        try (ActivityScenario<NotesListActivity> scenario = launchActivity()) {
            waitForNotesList(scenario, 1);
            scenario.onActivity(activity ->
                    activity.getOnBackPressedDispatcher().onBackPressed());

            verify(appSecurityService, timeout(5000)).logout();
        }
    }

    @Test
    public void testSupportHomeButtonFinishesActivity() {
        when(noteService.getAll()).thenReturn(List.of(createTestNote()));

        try (ActivityScenario<NotesListActivity> scenario = launchActivity()) {
            waitForNotesList(scenario, 1);
            scenario.onActivity(activity -> {
                MenuItem menuItem = mock(MenuItem.class);

                when(menuItem.getItemId()).thenReturn(android.R.id.home);

                assertTrue(activity.onOptionsItemSelected(menuItem));
                assertTrue(activity.isFinishing());
            });
        }
    }

    private ActivityScenario<NotesListActivity> launchActivity() {
        Intent intent = new Intent(context, NotesListActivity.class);
        return ActivityScenario.launch(intent);
    }

    private void waitForNotesList(ActivityScenario<NotesListActivity> scenario, int count) {
        scenario.onActivity(activity -> {
            ListView notesListView = activity.findViewById(R.id.notesListView);

            assertNotNull("Notes list adapter should be initialized",
                    notesListView.getAdapter());
            assertEquals(count, notesListView.getAdapter().getCount());
            assertEquals(count == 0 ? View.VISIBLE : View.INVISIBLE,
                    activity.findViewById(R.id.missingNotesLabel).getVisibility());
        });
    }

    private Note createTestNote() {
        Note note = new Note();

        note.setId(NOTE_ID);
        note.setName(NOTE_NAME);
        note.setText(NOTE_TEXT);
        note.setUpdatedAt(LocalDateTime.now());
        note.setDecimalId(NOTE_DECIMAL_ID);

        return note;
    }
}
