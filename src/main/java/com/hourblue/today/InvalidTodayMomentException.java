package com.hourblue.today;

public class InvalidTodayMomentException extends RuntimeException {

    public InvalidTodayMomentException(String message) {
        super(message);
    }

    public InvalidTodayMomentException(String message, Throwable cause) {
        super(message, cause);
    }
}
