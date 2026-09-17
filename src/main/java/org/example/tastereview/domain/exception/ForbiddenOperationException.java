package org.example.tastereview.domain.exception;

public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException() {
        super("권한이 없습니다");
    }

    public ForbiddenOperationException(String message) {
        super(message);
    }
}