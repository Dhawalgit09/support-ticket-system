package com.supportticket.exception;

public class FieldValidationException extends RuntimeException {

    private final String field;
    private final Object rejectedValue;

    public FieldValidationException(String field, String message, Object rejectedValue) {
        super(message);
        this.field = field;
        this.rejectedValue = rejectedValue;
    }

    public String field() {
        return field;
    }

    public Object rejectedValue() {
        return rejectedValue;
    }
}
