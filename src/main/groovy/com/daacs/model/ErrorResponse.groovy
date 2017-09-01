package com.daacs.model

import com.daacs.framework.exception.ErrorContainerException
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
/**
 * Created by alandistasio on 1/2/15.
 */

@JsonIgnoreProperties(["metaClass"])
public class ErrorResponse<T> {


    private List<ErrorContainer> errors = [];

    public ErrorResponse(List<ErrorContainer> errors) {
        this.errors = errors;
    }

    public ErrorResponse() {
    }

    public List<ErrorContainer> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorContainer> errors) {
        this.errors = errors;
    }

    public void addError(ErrorContainer error) {
        errors.add(error);
    }

    public void addErrors(ErrorContainerException errorContainerException){
            errors.addAll(errorContainerException.getErrorContainers());
    }
}
