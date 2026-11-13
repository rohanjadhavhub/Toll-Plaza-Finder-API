package com.tollfinder.application;

public class FinderException extends RuntimeException {
    public enum Reason { INVALID_PINCODE, NO_ROUTE, UPSTREAM_FAILURE, UNAVAILABLE, TIMEOUT }

    private final Reason reason;

    public FinderException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
