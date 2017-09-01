package com.daacs.service.hystrix;

import com.daacs.framework.exception.HystrixFileSystemException;
import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;
import com.daacs.framework.hystrix.GavantHystrixCommand;
import com.lambdista.util.Try;
import com.opencsv.CSVReader;

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
public class ReadCsvHystrixCommand extends GavantHystrixCommand<List<String[]>>{

    protected Path file;

    public ReadCsvHystrixCommand(String hystrixGroupKey, String hystrixCommandKey, Path file) {
        super(hystrixGroupKey, hystrixCommandKey);
        this.file = file;
    }

    @Override
    protected String getResourceName() {
        return "csvReader";
    }

    @Override
    protected Try<List<String[]>> run() throws Exception {
        try {
            CSVReader reader = new CSVReader(Files.newBufferedReader(file));
            List<String[]> csvLines = reader.readAll();

            return createSuccess(csvLines);
        }
        catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

    @Override
    protected Try<List<String[]>> failedExecutionFallback(Throwable t){
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