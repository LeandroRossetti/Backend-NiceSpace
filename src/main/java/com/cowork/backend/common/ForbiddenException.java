package com.cowork.backend.common;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }

    public HttpStatus getStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
