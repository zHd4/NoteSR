package com.peew.notesr.exception;

public class ImportFailedException extends Exception {
    public ImportFailedException() {}
    public ImportFailedException(Exception e) {
        super(e);
    }
}
