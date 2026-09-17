package org.example.tastereview.domain.exception;

public class DuplicateNicknameException extends RuntimeException {

    public DuplicateNicknameException(String message) {
        super(message);
    }
}