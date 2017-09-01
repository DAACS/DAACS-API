package com.daacs.unit.framework.validation

import com.daacs.framework.validation.DomainScoreValidator
import com.daacs.model.assessment.user.CompletionScore
import com.daacs.model.assessment.user.DomainScore
import com.daacs.unit.framework.validation.child.ValidatorSpec
/**
 * Created by chostetter on 4/10/17.
 */
class DomainScoreValidatorSpec extends ValidatorSpec {

    DomainScoreValidator validator
    DomainScore domainScore

    def setup(){
        setupContext()
        domainScore = new DomainScore()
        validator = new DomainScoreValidator()
    }

    def "isValid"(){
        setup:
        domainScore.setRubricScore(CompletionScore.MEDIUM)

        when:
        boolean isValid = validator.isValid(domainScore, context)

        then:
        isValid
    }

    def "isValid #2"(){
        setup:
        domainScore.setSubDomainScores([new DomainScore()])

        when:
        boolean isValid = validator.isValid(domainScore, context)

        then:
        isValid
    }

    def "!isValid"(){
        when:
        boolean isValid = validator.isValid(domainScore, context)

        then:
        !isValid
    }
}
