package com.daacs.model.dto.assessmentUpdate

import com.daacs.model.assessment.DomainType
/**
 * Created by chostetter on 2/17/17.
 */
class AnalysisDomainRequest extends DomainRequest {
    AnalysisDomainRequest() {
        domainType = DomainType.ANALYSIS;
    }
}
