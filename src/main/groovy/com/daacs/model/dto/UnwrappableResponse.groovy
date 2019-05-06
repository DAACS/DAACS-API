package com.daacs.model.dto

import com.daacs.model.ErrorContainer
import com.daacs.model.assessment.Assessment

/**
 * Created by lhorne on 9/20/17.
 */
class UnwrappableResponse {

}

class AssessmentResponse extends UnwrappableResponse {

    Assessment data
    List<ErrorContainer> meta = [];

    AssessmentResponse(Assessment data, List<ErrorContainer> meta) {
        this.data = data
        this.meta = meta
    }
}
