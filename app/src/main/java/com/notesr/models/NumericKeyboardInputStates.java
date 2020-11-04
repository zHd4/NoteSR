package com.notesr.models;

public enum NumericKeyboardInputStates {
    NUMERIC(0), SYMBOL1(1), SYMBOL2(2), SYMBOL3(3);

    private final int state;

    NumericKeyboardInputStates(final int state) {
        this.state = state;
    }

    public int getState() {
        return this.state;
    }
}
