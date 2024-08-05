package com.peew.notesr.exception;

public class InvalidDumpFormatException extends RuntimeException {
    public InvalidDumpFormatException() {}

    public InvalidDumpFormatException(String message) {
        super(message);
    }

    public InvalidDumpFormatException(Exception e) {
        super(e);
    }
}
