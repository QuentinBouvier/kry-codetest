package se.kry.codetest.exception;

import lombok.Getter;

public abstract class ControllerException extends RuntimeException {
    @Getter
    int code;

    public ControllerException() { super(); }

    public ControllerException(String message) { super(message); }
}
