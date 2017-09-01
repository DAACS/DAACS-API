package com.daacs.service.hystrix;

import com.daacs.framework.exception.HystrixShellCommandFailedException;
import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;
import com.daacs.framework.hystrix.GavantHystrixCommand;
import com.lambdista.util.Try;

import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static com.daacs.framework.hystrix.FailureType.NOT_RETRYABLE;
import static com.daacs.framework.hystrix.FailureType.RETRYABLE;

/**
 * Created by chostetter on 6/23/16.
 */
public class ExecuteCommandHystrixCommand extends GavantHystrixCommand<String>{

    protected String command;
    protected File workingDir;
    protected String[] envVars;

    public ExecuteCommandHystrixCommand(String hystrixGroupKey, String hystrixCommandKey, String command, String[] envVars, File workingDir) {
        super(hystrixGroupKey, hystrixCommandKey);
        this.command = command;
        this.workingDir = workingDir;
        this.envVars = envVars;
    }

    @Override
    protected String getResourceName() {
        return "shell";
    }

    @Override
    protected Try<String> run() throws Exception {
        try {

            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();
            Process p;

            p = Runtime.getRuntime().exec(command, envVars, workingDir);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
                output.append("\n");
            }

            reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            while ((line = reader.readLine()) != null) {
                error.append(line);
                error.append("\n");
            }

            if(error.length() > 0){
                return createFailure(new ScriptException(error.toString()), NOT_RETRYABLE);
            }

            return createSuccess(output.toString());
        }
        catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

    @Override
    protected Try<String> failedExecutionFallback(Throwable t){
        if(t instanceof IOException){
            return createFailure(t, RETRYABLE);
        }

        return createFailure(t, NOT_RETRYABLE);
    }

    @Override
    protected FailureTypeException buildFailureRequestException(String resourceName, String hystrixCommandKey, FailureType failureType, Throwable t){
        Map<String, Object> metaData = new HashMap<String, Object>(){{
            put("command", command);
            put("workingDir", workingDir);
            put("envVars", envVars);
        }};

        return new HystrixShellCommandFailedException(resourceName, hystrixCommandKey, failureType, metaData, t);
    }
}