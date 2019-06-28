package com.daacs.service;

import com.daacs.component.HystrixCommandFactory;
import com.daacs.framework.auth.repository.UniversalAuthenticationRepository;
import com.daacs.framework.exception.BadInputException;
import com.daacs.framework.exception.NotFoundException;
import com.daacs.framework.hystrix.FailureTypeException;
import com.daacs.model.Nonce;
import com.daacs.model.lti.OutcomeParams;
import com.daacs.repository.lti.LtiNonceRepository;
import com.daacs.repository.lti.OutcomeServiceRepositoryImpl;
import com.lambdista.util.Try;
import org.apache.commons.lang3.StringUtils;
import org.imsglobal.lti.launch.LtiVerificationResult;
import org.imsglobal.lti.launch.LtiVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by mgoldman on 2/26/19.
 */

@Service
public class LtiServiceImpl implements LtiService {
    private static final Logger logger = LoggerFactory.getLogger(LtiServiceImpl.class);

    private static final String GRANT_TYPE = "lti";

    @Value("${lti.oauth.version}")
    private String oauthVersion;

    @Value("${lti.oauth.signature_method}")
    private String signatureMethod;

    @Value("${lti.oath.timeout_interval}")
    private int timeoutInterval;

    @Value("${lti.oauth.sharedSecret}")
    private String secretKey;

    @Value("${canvas.enabled}")
    private boolean canvasEnabled;

    @Value("${lti.enabled}")
    private boolean enabled;

    @Autowired
    private LtiNonceRepository ltiNonceRepository;

    @Autowired
    private LtiVerifier ltiVerifier;

    @Autowired
    private UniversalAuthenticationRepository authenticationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private HystrixCommandFactory hystrixCommandFactory;

    @Autowired
    private OutcomeServiceRepositoryImpl outcomeServiceRepository;

    @Override
    public boolean isEnabled(){
        return enabled;
    }

    @Override
    public Try<String> verifyLaunch(HttpServletRequest launchRequest) {

        String response;

        Try<String> maybeVerified = verifyOauth(launchRequest);
        if (maybeVerified.isFailure()) {
            return new Try.Failure<>(maybeVerified.failed().get());
        }

        response = (maybeVerified.get());
        return new Try.Success<>(response);
    }

    private Try<String> verifyOauth(HttpServletRequest launchRequest) {

        if (!StringUtils.equals(launchRequest.getParameter("oauth_signature_method"), signatureMethod)) {
            return new Try.Failure<>(new BadInputException("oauth_signature_method", "invalid"));

        }
        if (!StringUtils.equals(launchRequest.getParameter("oauth_version"), oauthVersion)) {
            return new Try.Failure<>(new BadInputException("oauth_version", "invalid"));
        }
        if (timeDiffInMinutes(System.currentTimeMillis() / 1000L, launchRequest.getParameter("oauth_timestamp")) > timeoutInterval) {
            return new Try.Failure<>(new BadInputException("oauth_timestamp", "expired"));
        }

        //nonceVerification
        Try<Boolean> maybeNonceVerified = verifyNonce(launchRequest.getParameter("oauth_nonce"));
        if (maybeNonceVerified.isFailure()) {
            return new Try.Failure<>(maybeNonceVerified.failed().get());
        }

        LtiVerificationResult ltiResult;
        try {
            ltiResult = ltiVerifier.verify(launchRequest, secretKey);
        } catch (Exception e) {
            return new Try.Failure<>(e);
        }

        if (!ltiResult.getSuccess()) {
            return new Try.Failure<>(new BadInputException("lti verification error", ltiResult.getMessage()));
        }
        String accessToken;
        try {
            //TODO: this might need to be changed depending on how canvas lti params work
            accessToken = getAuthentication(launchRequest);
        } catch (Exception e) {
            return new Try.Failure<>(e);
        }

        return new Try.Success<>(accessToken);
    }

    private long timeDiffInMinutes(Long currentTime, String oldTime) {
        long diffSeconds = currentTime - Long.parseLong(oldTime);
        return (diffSeconds / 60);
    }

    private Try<Boolean> verifyNonce(String id) {

        Nonce nonce = new Nonce(id);
        Try<Void> maybeInserted = ltiNonceRepository.insertNonce(nonce);
        if (maybeInserted.isFailure()) {
            return new Try.Failure<>(maybeInserted.failed().get());
        }

        Try<Nonce> maybeNonce = ltiNonceRepository.getNonce(id);
        if (maybeNonce.isFailure()) {
            return new Try.Failure<>(maybeNonce.failed().get());
        }

        return new Try.Success<>(maybeNonce.get().getId().equals(id));
    }

    //if lti is verified; create a new authentication token, exchange for a key, and store token in auth repository
    private String getAuthentication(HttpServletRequest launchRequest) throws InvalidGrantException {

        String username = launchRequest.getParameter("custom_username");

        if (StringUtils.isEmpty(username)) {
            throw new InvalidGrantException("username is required");
        }

        //Get the user.
        UserDetails userDetails;
        try {
            userDetails = userService.loadUserByUsername(username);
        } catch (FailureTypeException e) {
            throw new InvalidGrantException(e.getMessage());
        }

        if(canvasEnabled){
            //verify grading params
            verifySourcedId(launchRequest, username);
        }

        Authentication authentication = new AnonymousAuthenticationToken(GRANT_TYPE, userDetails, userDetails.getAuthorities());
        String token = authenticationRepository.storeAuthenitcation(authentication);

        return token;
    }

    private void verifySourcedId(HttpServletRequest launchRequest, String username) {

        String oauth_consumer_key = launchRequest.getParameter("oauth_consumer_key");
        if (StringUtils.isEmpty(oauth_consumer_key)){
            throw new BadInputException("oauth_consumer_key", "empty");
        }

        String lis_outcome_service_url = launchRequest.getParameter("lis_outcome_service_url");
        if (StringUtils.isEmpty(lis_outcome_service_url)){
            throw new BadInputException("lis_outcome_service_url", "empty");
        }

        String lis_result_sourcedid = launchRequest.getParameter("lis_result_sourcedid");
        if (StringUtils.isEmpty(lis_result_sourcedid)){
            throw new BadInputException("lis_result_sourcedid", "empty");
        }


        //store params
        String id = userService.getUserByUsername(username).get().getId();
        outcomeServiceRepository.storeOutcomeParams(new OutcomeParams(oauth_consumer_key, lis_outcome_service_url, lis_result_sourcedid),id);
    }

    @Override
    public Try<Void> updateGrades(String userId) {
        OutcomeParams params = null;
        try {
            params = outcomeServiceRepository.retrieveOutcomeParams(userId);
        }catch ( NotFoundException e){
            return new Try.Failure<>(e);
        }
        return hystrixCommandFactory.getLTIReplaceResultCommand("LtiServiceImpl-updateGrades", params.getUrl(), params.getConsumerKey(), secretKey, params.getSourcedid(), "1").execute();
    }
}
