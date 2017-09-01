package com.daacs.model.dto

import com.daacs.model.assessment.ScoringType

/**
 * Created by chostetter on 8/24/16.
 */
enum ScoringClass {
    MANUAL([ScoringType.MANUAL]),
    AUTO([ScoringType.AVERAGE, ScoringType.SUM, ScoringType.LIGHTSIDE])

    private List<ScoringType> scoringTypes;
    ScoringClass(List<ScoringType> scoringTypes){
        this.scoringTypes = scoringTypes;
    }

    public List<ScoringType> getScoringTypes(){
        return scoringTypes;
    }
}