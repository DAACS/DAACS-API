package com.daacs.repository.lti;

import com.daacs.framework.exception.NotFoundException;
import com.daacs.model.lti.OutcomeParams;

/**
 * Created by mgoldman on 6/7/19.
 */
public interface OutcomeServiceRepository {
    String storeOutcomeParams(OutcomeParams params, String userId);
    OutcomeParams retrieveOutcomeParams(String userId) throws NotFoundException;
}
