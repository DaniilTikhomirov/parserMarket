package com.example.demo.exeptions;

public class FailureUrl extends RuntimeException {
    public FailureUrl(String message) {
        super(message);
    }
}
