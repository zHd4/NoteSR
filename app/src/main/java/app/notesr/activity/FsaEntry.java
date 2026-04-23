/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity;

import app.notesr.service.AndroidService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entry representing Foreground Service Association (FSA).
 * Represents a mapping between a foreground service and an activity.
 * <p>
 * This class is used to associate a specific {@link AndroidService} that runs in the foreground
 * with its corresponding {@link ActivityBase}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class FsaEntry {

    /**
     * The class of the foreground service.
     */
    private Class<? extends AndroidService> foregroundServiceClass;

    /**
     * The class of the activity associated with the foreground service.
     */
    private Class<? extends ActivityBase> activityClass;

}
