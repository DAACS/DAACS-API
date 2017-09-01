package com.daacs.unit.framework.validation

import com.daacs.framework.validation.UpdateUserAssessmentRequestValidator
import com.daacs.model.assessment.user.CompletionScore
import com.daacs.model.assessment.user.CompletionStatus
import com.daacs.model.assessment.user.DomainScore
import com.daacs.model.dto.UpdateUserAssessmentRequest
import com.daacs.unit.framework.validation.child.ValidatorSpec
/**
 * Created by chostetter on 8/23/16.
 */
class UpdateUserAssessmentRequestValidatorSpec extends ValidatorSpec {

    UpdateUserAssessmentRequestValidator validator;
    UpdateUserAssessmentRequest updateUserAssessmentRequest;

    def setup(){
        setupContext()
        updateUserAssessmentRequest = new UpdateUserAssessmentRequest(
                id: "assessment-1",
                status: CompletionStatus.GRADED,
                userId: "user-1",
                domainScores: [ new DomainScore(domainId: "domain-1", rubricScore: CompletionScore.HIGH) ],
                overallScore: CompletionScore.HIGH
        )
        validator = new UpdateUserAssessmentRequestValidator()
    }

    def "isValid: true for COMPLETED"(){
        setup:
        updateUserAssessmentRequest.status = CompletionStatus.COMPLETED

        when:
        boolean isValid = validator.isValid(updateUserAssessmentRequest, context)

        then:
        isValid
    }

    def "isValid: true for GRADED"(){
        when:
        boolean isValid = validator.isValid(updateUserAssessmentRequest, context)

        then:
        isValid
    }

    def "isValid: false when userId is null"(){
        setup:
        updateUserAssessmentRequest.userId = null

        when:
        boolean isValid = validator.isValid(updateUserAssessmentRequest, context)

        then:
        !isValid
    }

    def "isValid: false when domainScores is null"(){
        setup:
        updateUserAssessmentRequest.domainScores = null

        when:
        boolean isValid = validator.isValid(updateUserAssessmentRequest, context)

        then:
        !isValid
    }
}
