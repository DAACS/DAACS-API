package com.daacs.framework.exception;

/**
 * Created by alandistasio on 10/23/15.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
