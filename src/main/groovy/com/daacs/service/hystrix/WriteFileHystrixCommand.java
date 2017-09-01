package com.daacs.service.hystrix;

import com.daacs.framework.exception.HystrixFileSystemException;
import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;
import com.daacs.framework.hystrix.GavantHystrixCommand;
import com.lambdista.util.Try;
import org.apache.commons.io.IOUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.daacs.framework.hystrix.FailureType.NOT_RETRYABLE;
import static com.daacs.framework.hystrix.FailureType.RETRYABLE;

/**
 * Created by chostetter on 9/1/16.
 */
public class WriteFileHystrixCommand extends GavantHystrixCommand<Void>{

    protected Path file;
    protected InputStream inputStream;

    public WriteFileHystrixCommand(String hystrixGroupKey, String hystrixCommandKey, InputStream inputStream, Path file) {
        super(hystrixGroupKey, hystrixCommandKey);
        this.file = file;
        this.inputStream = inputStream;
    }

    @Override
    protected String getResourceName() {
        return "IOUtils";
    }

    @Override
    protected Try<Void> run() throws Exception {
        try {
            OutputStream outputStream = new FileOutputStream(file.toFile());
            IOUtils.copy(inputStream, outputStream);
            return createSuccess(null);
        }
        catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

    @Override
    protected Try<Void> failedExecutionFallback(Throwable t){
        if(t instanceof FileAlreadyExistsException){
            return createFailure(t, NOT_RETRYABLE);
        }

        if(t instanceof SecurityException){
            return createFailure(t, NOT_RETRYABLE);
        }

        if(t instanceof IOException){
            return createFailure(t, RETRYABLE);
        }

        return createFailure(t, NOT_RETRYABLE);
    }

    @Override
    protected FailureTypeException buildFailureRequestException(String resourceName, String hystrixCommandKey, FailureType failureType, Throwable t){
        Map<String, Object> metaData = new HashMap<String, Object>(){{
            put("file", file);
        }};

        return new HystrixFileSystemException(resourceName, hystrixCommandKey, failureType, metaData, t);
    }
}