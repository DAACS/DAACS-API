package com.daacs.unit.framework.validation.child.writing

import com.daacs.framework.validation.child.writing.LightSideConfigValidator
import com.daacs.model.assessment.LightSideConfig
import com.daacs.model.assessment.ScoringDomain
import com.daacs.model.assessment.ScoringType
import com.daacs.model.assessment.WritingAssessment
import com.daacs.unit.framework.validation.child.ValidatorSpec
/**
 * Created by chostetter on 8/22/16.
 */
class LightSideConfigValidatorSpec extends ValidatorSpec {
    LightSideConfigValidator validator;
    WritingAssessment writingAssessment;

    def setup(){
        setupContext()
        validator = new LightSideConfigValidator()
        writingAssessment = new WritingAssessment(
                domains: [
                        new ScoringDomain(id: "domain-1"),
                        new ScoringDomain(id: "domain-2")
                ],
                scoringType: ScoringType.LIGHTSIDE,
                lightSideConfig: new LightSideConfig(
                        domainModels: [
                                "domain-1": "domain-1.xml",
                                "domain-2": "domain-2.xml"
                        ]
                )
        )
    }

    def "isValid: passes w/LIGHTSIDE scoringType"(){
        when:
        boolean isValid = validator.isValid(writingAssessment, context)

        then:
        isValid
    }

    def "isValid: passes w/MANUAL scoringType"(){
        setup:
        writingAssessment.scoringType = ScoringType.MANUAL

        when:
        boolean isValid = validator.isValid(writingAssessment, context)

        then:
        isValid
    }

    def "isValid: fails when lightSideConfig is null"(){
        setup:
        writingAssessment.lightSideConfig = null

        when:
        boolean isValid = validator.isValid(writingAssessment, context)

        then:
        !isValid
    }

    def "isValid: fails when lightSideConfig doesn't have all domains"(){
        setup:
        writingAssessment.lightSideConfig.domainModels = [:]

        when:
        boolean isValid = validator.isValid(writingAssessment, context)

        then:
        !isValid
    }
}