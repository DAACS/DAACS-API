package com.daacs.model.lti

/**
 * Created by mgoldman on 6/7/19.
 */

public class OutcomeParams {

    OutcomeParams(String consumerKey, String url, String sourcedid) {
        this.consumerKey = consumerKey
        this.url = url
        this.sourcedid = sourcedid
    }

    String consumerKey
    String url
    String sourcedid
}
