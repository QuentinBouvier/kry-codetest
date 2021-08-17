package se.kry.codetest.exception;

public class ResourceNotFoundException extends ControllerException {

    public ResourceNotFoundException() {
        super();
        this.code = 404;
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.code = 404;
    }
}
