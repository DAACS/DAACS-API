package com.daacs.model.assessment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
/**
 * Created by chostetter on 2/16/17.
 */
@JsonIgnoreProperties(["metaClass"])
class AnalysisDomain extends Domain {
    AnalysisDomain() {
        domainType = DomainType.ANALYSIS;
    }
}
