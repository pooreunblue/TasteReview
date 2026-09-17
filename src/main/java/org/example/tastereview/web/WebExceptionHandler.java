package org.example.tastereview.web;

import org.example.tastereview.domain.exception.CommentNotFoundException;
import org.example.tastereview.domain.exception.ForbiddenOperationException;
import org.example.tastereview.domain.exception.MemberNotFoundException;
import org.example.tastereview.domain.exception.ReviewNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class WebExceptionHandler {

    @ExceptionHandler({ReviewNotFoundException.class, CommentNotFoundException.class,
            MemberNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFound() {
        return new ModelAndView("error/404");
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleForbidden() {
        return new ModelAndView("error/403");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleUploadSizeExceeded() {
        return new ModelAndView("error/file-too-large");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleUnexpected() {
        return new ModelAndView("error/500");
    }
}