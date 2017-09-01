package com.daacs.service.hystrix;

import com.daacs.framework.exception.HystrixFileSystemException;
import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;
import com.daacs.framework.hystrix.GavantHystrixCommand;
import com.lambdista.util.Try;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.daacs.framework.hystrix.FailureType.NOT_RETRYABLE;
import static com.daacs.framework.hystrix.FailureType.RETRYABLE;

/**
 * Created by chostetter on 6/23/16.
 */
public class LightSideInputCheckHystrixCommand extends GavantHystrixCommand<Void>{

    private Path modelFile;
    private Path inputFile;
    private Path outputFile;

    public LightSideInputCheckHystrixCommand(String hystrixGroupKey, String hystrixCommandKey, Path modelFile, Path inputFile, Path outputFile) {
        super(hystrixGroupKey, hystrixCommandKey);
        this.modelFile = modelFile;
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    @Override
    protected String getResourceName() {
        return "lightSide";
    }

    @Override
    protected Try<Void> run() throws Exception {
        try {

            if(!Files.exists(modelFile)){
                return createFailure(new NoSuchFileException(modelFile.toString()), NOT_RETRYABLE);
            }

            if(!Files.isReadable(modelFile)){
                return createFailure(new SecurityException(modelFile.toString()), NOT_RETRYABLE);
            }

            if(!Files.exists(inputFile)){
                return createFailure(new NoSuchFileException(inputFile.toString()), NOT_RETRYABLE);
            }

            if(!Files.isReadable(inputFile)){
                return createFailure(new SecurityException(inputFile.toString()), NOT_RETRYABLE);
            }

            Files.deleteIfExists(outputFile);

            return createSuccess(null);
        }
        catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

    @Override
    protected Try<Void> failedExecutionFallback(Throwable t){
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
            put("modelFile", modelFile);
            put("inputFile", inputFile);
        }};

        return new HystrixFileSystemException(resourceName, hystrixCommandKey, failureType, metaData, t);
    }
}