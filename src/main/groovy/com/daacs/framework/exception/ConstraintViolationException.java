package com.daacs.framework.exception;

import com.daacs.model.ErrorContainer;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by chostetter on 7/25/16.
 */
public class ConstraintViolationException extends ErrorContainerException {

    public ConstraintViolationException(List<ObjectError> errors) {
        super("Constraint.violation", "Problem validating object", buildErrorContainers(errors));
    }

    private static List<ErrorContainer> buildErrorContainers(List<ObjectError> errors){
        return errors.stream()
                .map(ConstraintViolationException::buildErrorContainer)
                .collect(Collectors.toList());
    }

    private static ErrorContainer buildErrorContainer(ObjectError objectError){
        ErrorContainer errorContainer = new ErrorContainer();
        errorContainer.setCode(objectError.getObjectName() + ".constraintViolation");

        Map<String, Object> meta = new HashMap<>();

        if(objectError instanceof FieldError){
            errorContainer.setDetail(((FieldError) objectError).getField() + " " + objectError.getDefaultMessage());
            meta.put("field", ((FieldError) objectError).getField());
            meta.put("code", objectError.getCode());
        }
        else{
            errorContainer.setDetail(objectError.getDefaultMessage());
            meta.put("arguments", objectError.getArguments());
            meta.put("codes", objectError.getArguments());
        }

        meta.put("default_message", objectError.getDefaultMessage());
        meta.put("object_name", objectError.getObjectName());

        errorContainer.setMeta(meta);

        return errorContainer;
    }
}


