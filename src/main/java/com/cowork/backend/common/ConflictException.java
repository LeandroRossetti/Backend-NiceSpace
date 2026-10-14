package com.cowork.backend.common;

import org.springframework.http.HttpStatus;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }

    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
