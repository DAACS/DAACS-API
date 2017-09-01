package com.daacs.framework.exception;

/**
 * Created by alandistasio on 10/23/15.
 */
public class EndpointDisabledException extends RuntimeException {

    public EndpointDisabledException(String message) {
        super(message);
    }
}
