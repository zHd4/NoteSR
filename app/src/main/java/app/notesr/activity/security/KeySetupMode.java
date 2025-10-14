package app.notesr.activity.security;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum KeySetupMode {
    FIRST_RUN("first_run"),
    REGENERATION("regeneration");

    public final String mode;
}
