package com.notesr.models;

public enum NumericKeyboardInputStates {
    NUMERIC('1'), SYMBOL1('A'), SYMBOL2('B'), SYMBOL3('C');

    private final char state;

    NumericKeyboardInputStates(final char state) {
        this.state = state;
    }

    public char getState() {
        return this.state;
    }
}
