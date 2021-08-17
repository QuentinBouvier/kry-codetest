package se.kry.codetest.exception;

public class BadRequestException extends ControllerException {

    public BadRequestException() {
        super();
        this.code = 400;
    }

    public BadRequestException(String message) {
        super(message);
        this.code = 400;
    }
}
