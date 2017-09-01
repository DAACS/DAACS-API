package com.daacs.component.queuing;

import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;
import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by chostetter on 8/9/16.
 */
public class Retry {

    private static final Logger log = LoggerFactory.getLogger(Retry.class);

    private final int waitSeconds;
    private final int maxRetries;

    public Retry(int waitSeconds, int maxRetries) {
        this.waitSeconds = waitSeconds;
        this.maxRetries = maxRetries;
    }

    public <T> T execute(Retryable<Try<T>> retryable) throws Exception {

        Throwable throwable = null;

        for(int attempts = 0; attempts < (maxRetries < 0 ? attempts+1 : maxRetries); attempts++) {
            Try<T> result = retryable.get();

            if (result.isSuccess()) {
                return result.get();
            }

            throwable = result.failed().get();

            if(!(throwable instanceof FailureTypeException)){
                throw new Exception(throwable);
            }

            if (((FailureTypeException) throwable).getFailureType() != FailureType.RETRYABLE) {
                throw (FailureTypeException) throwable;
            }

            if(attempts < (maxRetries < 0 ? attempts+1 : maxRetries)){
                log.info("Retry attempts: {}/{}", attempts, maxRetries, throwable);
                log.info("Waiting {} seconds before next retry...", waitSeconds);

                try {
                    Thread.sleep(waitSeconds * 1000);
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        String maxReachedMsg = "Maximum retry attempts reached.";
        if(throwable == null){
            throwable = new Exception(maxReachedMsg);
        }

        log.error(maxReachedMsg);
        throw new Exception(throwable);
    }
}