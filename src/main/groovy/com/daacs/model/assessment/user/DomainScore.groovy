package com.daacs.model.assessment.user

import com.daacs.framework.validation.annotations.ValidDomainScore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import javax.validation.Valid
import javax.validation.constraints.NotNull
/**
 * Created by chostetter on 7/6/16.
 */

@JsonIgnoreProperties(["metaClass"])
@ValidDomainScore
class DomainScore {
    @NotNull
    String domainId;

    CompletionScore rubricScore;

    Double rawScore;

    @Valid
    List<DomainScore> subDomainScores;


    @Override
    public String toString() {
        return "DomainScore{" +
                "domainId='" + domainId + '\'' +
                ", rubricScore=" + rubricScore +
                ", rawScore=" + rawScore +
                ", subDomainScores=" + subDomainScores +
                '}';
    }
}
