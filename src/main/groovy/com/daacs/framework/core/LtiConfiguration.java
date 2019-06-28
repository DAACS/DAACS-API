package com.daacs.framework.core;

import org.imsglobal.lti.launch.LtiOauthVerifier;
import org.imsglobal.lti.launch.LtiVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by mgoldman on 2/27/19.
 */
@Configuration
public class LtiConfiguration {

    @Bean
    public LtiVerifier ltiVerifier() {
        return new LtiOauthVerifier();
    }
}
