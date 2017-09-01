package com.daacs.service.hystrix;

import com.daacs.framework.exception.HystrixFileSystemException;
import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;
import com.daacs.framework.hystrix.GavantHystrixCommand;
import com.lambdista.util.Try;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
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
public class LightSideDirCheckHystrixCommand extends GavantHystrixCommand<Void>{

    private Path lightSideModelsDir;
    private Path lightSideOutputDir;
    private Path predictScript;

    public LightSideDirCheckHystrixCommand(
            String hystrixGroupKey,
            String hystrixCommandKey,
            Path lightSideModelsDir,
            Path lightSideOutputDir,
            Path predictScript) {

        super(hystrixGroupKey, hystrixCommandKey);
        this.lightSideModelsDir = lightSideModelsDir;
        this.lightSideOutputDir = lightSideOutputDir;
        this.predictScript = predictScript;
    }

    @Override
    protected String getResourceName() {
        return "lightSide";
    }

    @Override
    protected Try<Void> run() throws Exception {
        try {

            if(!Files.exists(predictScript)){
                return createFailure(new NoSuchFileException(predictScript.toString()), NOT_RETRYABLE);
            }

            if(!Files.isExecutable(predictScript)){
                return createFailure(new SecurityException(predictScript.toString() + " does not have executable permissions"), NOT_RETRYABLE);
            }

            if(!Files.exists(lightSideModelsDir)){
                Files.createDirectories(lightSideModelsDir);
            }

            if(!Files.isDirectory(lightSideModelsDir)){
                return createFailure(new NoSuchFileException(lightSideModelsDir.toString()), NOT_RETRYABLE);
            }

            if(!Files.isReadable(lightSideModelsDir)){
                return createFailure(new SecurityException(lightSideModelsDir.toString() + " does not have read permissions"), NOT_RETRYABLE);
            }

            if(!Files.exists(lightSideOutputDir)){
                Files.createDirectories(lightSideOutputDir);
            }

            if(!Files.isDirectory(lightSideOutputDir)){
                return createFailure(new NoSuchFileException(lightSideOutputDir.toString()), NOT_RETRYABLE);
            }

            if(!Files.isReadable(lightSideOutputDir)){
                return createFailure(new SecurityException(lightSideOutputDir.toString() + " does not have read permissions"), NOT_RETRYABLE);
            }

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
            put("lightSideModelsDir", lightSideModelsDir);
            put("lightSideOutputDir", lightSideOutputDir);
            put("predictScript", predictScript);
        }};

        return new HystrixFileSystemException(resourceName, hystrixCommandKey, failureType, metaData, t);
    }
}