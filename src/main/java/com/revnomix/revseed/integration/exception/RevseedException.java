package com.revnomix.revseed.integration.exception;


public class RevseedException extends RuntimeException{

    private static final long serialVersionUID = -2706843730474712057L;

    public RevseedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RevseedException(String message) {
        super(message);
    }
}
