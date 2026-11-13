package com.tollfinder.domain;

public record Pincode(String value) {
    public Pincode {
        if (value == null || !value.matches("[1-9][0-9]{5}")) {
            throw new IllegalArgumentException("Invalid source or destination pincode");
        }
    }
}
