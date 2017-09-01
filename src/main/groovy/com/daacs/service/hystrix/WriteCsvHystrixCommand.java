package com.daacs.service.hystrix;

import com.daacs.framework.exception.HystrixFileSystemException;
import com.daacs.framework.hystrix.*;
import com.lambdista.util.Try;
import com.opencsv.CSVWriter;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.daacs.framework.hystrix.FailureType.NOT_RETRYABLE;
import static com.daacs.framework.hystrix.FailureType.RETRYABLE;

/**
 * Created by chostetter on 6/23/16.
 */
public class WriteCsvHystrixCommand extends GavantHystrixCommand<Void>{

    protected Path file;
    protected List<String[]> writableLines;

    public WriteCsvHystrixCommand(String hystrixGroupKey, String hystrixCommandKey, Path file, List<String[]> writableLines) {
        super(hystrixGroupKey, hystrixCommandKey);
        this.file = file;
        this.writableLines = writableLines;
    }

    @Override
    protected String getResourceName() {
        return "csvWriter";
    }

    @Override
    protected Try<Void> run() throws Exception {
        try {
            CSVWriter writer = new CSVWriter(Files.newBufferedWriter(file), ',');
            writer.writeAll(writableLines);
            writer.close();

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
            put("writableLines", writableLines);
        }};

        return new HystrixFileSystemException(resourceName, hystrixCommandKey, failureType, metaData, t);
    }
}