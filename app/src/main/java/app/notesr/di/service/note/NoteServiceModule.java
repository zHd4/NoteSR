/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di.service.note;

import androidx.annotation.NonNull;

import app.notesr.data.AppDatabase;
import app.notesr.service.note.NoteService;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Dagger Hilt module for providing NoteService.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class NoteServiceModule {

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
