package org.example.tastereview.domain.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class ImageValidationException extends RuntimeException {

    private final List<String> messages;

    public ImageValidationException(List<String> messages) {
        super(String.join(System.lineSeparator(), messages));
        this.messages = messages;
    }
}