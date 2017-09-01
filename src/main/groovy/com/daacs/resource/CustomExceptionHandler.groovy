package com.daacs.resource

import com.daacs.framework.exception.ConstraintViolationException
import com.daacs.framework.exception.NotFoundException
import com.daacs.framework.exception.RepoNotFoundException
import com.daacs.framework.hystrix.FailureType
import com.daacs.framework.hystrix.FailureTypeException
import com.daacs.model.ErrorContainer
import com.daacs.model.ErrorResponse
import com.fasterxml.jackson.databind.JsonMappingException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus

import javax.servlet.http.HttpServletResponse
/**
 * Created by alandistasio on 1/2/15.
 */
@ControllerAdvice
public class CustomExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(CustomExceptionHandler.class);

    private final MessageSource messageSource;

    @Autowired
    public CustomExceptionHandler(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    ErrorResponse getErrorMessage(NotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse();

        log.debug(ex.getClass().getSimpleName(), ex);
        errorResponse.addError(new ErrorContainer(code: "unknown.notFound", detail: ex.getMessage()));
        return errorResponse;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    ErrorResponse getErrorMessage(UnauthorizedUserException ex) {
        ErrorResponse errorResponse = new ErrorResponse();

        errorResponse.addError(new ErrorContainer(code: "user.unauthorized", detail: ex.getMessage()));
        return errorResponse;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    ErrorResponse getErrorMessage(Exception ex) {
        ErrorResponse errorResponse = new ErrorResponse();

        log.error("UnknownError", ex);
        errorResponse.addError(new ErrorContainer(code: "unknown.unexpected", detail: ex.getMessage()));
        return errorResponse;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    ErrorResponse getErrorMessage(MethodArgumentNotValidException ex) {
        ErrorResponse errorResponse = new ErrorResponse();

        log.debug(ex.getClass().getSimpleName(), ex);

        errorResponse.addErrors(new ConstraintViolationException(ex.getBindingResult().getAllErrors()));
        return errorResponse;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    ErrorResponse getErrorMessage(ConstraintViolationException ex) {
        ErrorResponse errorResponse = new ErrorResponse();

        log.debug(ex.getClass().getSimpleName(), ex);

        errorResponse.addErrors(ex);
        return errorResponse;
    }


    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    ErrorResponse getErrorMessage(UnsatisfiedServletRequestParameterException ex) {
        ErrorResponse errorResponse = new ErrorResponse();

        log.debug(ex.getClass().getSimpleName(), ex);

        errorResponse.addError(
                new ErrorContainer(
                        code: "parameter.invalid",
                        detail: ex.getMessage(),
                        meta: [
                                "actual_parameters": ex.getActualParams(),
                                "param_conditions": ex.getParamConditions(),
                                "param_condition_groups": ex.getParamConditionGroups()
                        ])
        );
        return errorResponse;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    ErrorResponse getErrorMessage(JsonMappingException ex) {
        ErrorResponse errorResponse = new ErrorResponse();

        log.warn(ex.getClass().getSimpleName(), ex);

        errorResponse.addError(
                new ErrorContainer(
                        code: "json.invalid",
                        detail: ex.getMessage())
        );

        return errorResponse;
    }

    @ExceptionHandler
    @ResponseBody
    ErrorResponse getErrorMessage(FailureTypeException ex, HttpServletResponse response) {
        if (ex.log) {
            if (ex.getFailureType() == FailureType.RETRYABLE) {
                log.warn("FailureTypeException", ex);
            } else if (ex.getFailureType() == FailureType.NOT_RETRYABLE) {
                log.error("FailureTypeException", ex);
            }
        } else {
            log.debug("FailureTypeException", ex.getMessage());
        }

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.addErrors(ex);

        if(ex instanceof RepoNotFoundException){
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        else if(ex.getFailureType() == FailureType.RETRYABLE){
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
        else{
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        return errorResponse;
    }
}
