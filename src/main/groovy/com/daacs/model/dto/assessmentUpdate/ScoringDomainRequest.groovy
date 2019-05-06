package com.daacs.model.dto.assessmentUpdate

import com.daacs.model.assessment.DomainType

import javax.validation.Valid
import javax.validation.constraints.NotNull
/**
 * Created by chostetter on 2/17/17.
 */
class ScoringDomainRequest extends DomainRequest {
    @Valid
    @NotNull
    RubricRequest rubric

    @Valid
    List<DomainRequest> subDomains;

    @NotNull
    Boolean scoreIsSubDomainAverage = false;

    ScoringDomainRequest() {
        domainType = DomainType.SCORING;
    }
}
