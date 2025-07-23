package app.notesr.crypto;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum KeySetupMode {
    FIRST_RUN("first_run"),
    REGENERATION("regeneration");

    public final String mode;
}
