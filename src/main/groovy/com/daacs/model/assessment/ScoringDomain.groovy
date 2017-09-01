package com.daacs.model.assessment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import javax.validation.Valid
import javax.validation.constraints.NotNull

/**
 * Created by chostetter on 2/16/17.
 */
@JsonIgnoreProperties(["metaClass"])
class ScoringDomain extends Domain {
    @Valid
    Rubric rubric;

    @Valid
    List<Domain> subDomains;

    @NotNull
    Boolean scoreIsSubDomainAverage = false;

    ScoringDomain() {
        domainType = DomainType.SCORING;
    }

}
