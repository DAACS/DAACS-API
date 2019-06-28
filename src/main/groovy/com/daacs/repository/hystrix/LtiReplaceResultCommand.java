package com.daacs.repository.hystrix;

import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;
import com.daacs.framework.hystrix.GavantHystrixCommand;
import com.daacs.framework.hystrix.HystrixRequestException;
import com.lambdista.util.Try;
import org.imsglobal.pox.IMSPOXRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

import static com.daacs.framework.hystrix.FailureType.NOT_RETRYABLE;
import static com.daacs.framework.hystrix.FailureType.RETRYABLE;

/**
 * Created by mgoldman on 6/7/19.
 */

public class LtiReplaceResultCommand extends GavantHystrixCommand<Void> {
    private static final Logger log = LoggerFactory.getLogger(LtiReplaceResultCommand.class);

    private String lis_outcome_service_url;
    private String oauth_consumer_key;
    private String secretKey;
    private String lis_result_sourcedid;
    private String score;

    public LtiReplaceResultCommand(String hystrixGroupKey, String hystrixCommandKey, String lis_outcome_service_url, String oauth_consumer_key, String secretKey, String lis_result_sourcedid, String score) {
        super(hystrixGroupKey, hystrixCommandKey);
        this.lis_outcome_service_url = lis_outcome_service_url;
        this.oauth_consumer_key = oauth_consumer_key;
        this.secretKey = secretKey;
        this.lis_result_sourcedid = lis_result_sourcedid;
        this.score = score;
    }

    @Override
    protected String getResourceName() {
        return "lti";
    }

    @Override
    protected Try<Void> run() throws Exception {
        try {
            IMSPOXRequest.sendReplaceResult(lis_outcome_service_url, oauth_consumer_key, secretKey, lis_result_sourcedid, score);

            return createSuccess(null);
        } catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

    @Override
    protected Try<Void> failedExecutionFallback(Throwable t) {
        if (t instanceof SecurityException) {
            return createFailure(t, NOT_RETRYABLE);
        } else if (t instanceof IOException) {
            return createFailure(t, RETRYABLE);
        }
        return createFailure(t, NOT_RETRYABLE);
    }

    @Override
    protected FailureTypeException buildFailureRequestException(String resourceName, String hystrixCommandKey, FailureType failureType, Throwable t){
        return new HystrixRequestException(resourceName, hystrixCommandKey, failureType, t);
    }

}
