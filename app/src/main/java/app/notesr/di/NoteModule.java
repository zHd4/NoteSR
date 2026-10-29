/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di;

import androidx.annotation.NonNull;

import app.notesr.data.AppDatabase;
import app.notesr.service.note.NoteService;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Dagger Hilt module for providing note-related services.
 * <p>
 * This module provides dependencies for note management operations,
 * including CRUD operations and note data handling.
 */
@Module
@InstallIn(SingletonComponent.class)
public class NoteModule {

    /**
     * Provides the {@link NoteService} instance for note management operations.
     *
     * @param appDatabase the application database instance
     * @return the singleton NoteService instance
     */
    @Provides
    NoteService provideNoteService(@NonNull AppDatabase appDatabase) {
        return new NoteService(appDatabase);
    }
}
