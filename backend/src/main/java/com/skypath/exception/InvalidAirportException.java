package com.skypath.exception;

public class InvalidAirportException extends RuntimeException {
    public InvalidAirportException(String code) {
        super("Unknown airport code: " + code);
    }
}
