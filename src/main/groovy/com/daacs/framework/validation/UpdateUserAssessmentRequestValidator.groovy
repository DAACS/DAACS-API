package com.daacs.framework.validation

import com.daacs.framework.validation.annotations.ValidUpdateUserAssessmentRequest
import com.daacs.model.assessment.user.CompletionStatus
import com.daacs.model.dto.UpdateUserAssessmentRequest

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
/**
 * Created by chostetter on 8/19/16.
 */

class UpdateUserAssessmentRequestValidator extends AbstractValidator implements ConstraintValidator<ValidUpdateUserAssessmentRequest, UpdateUserAssessmentRequest> {

    @Override
    public void initialize(ValidUpdateUserAssessmentRequest constraintAnnotation) {}

    @Override
    public boolean isValid(UpdateUserAssessmentRequest updateUserAssessmentRequest, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        if(updateUserAssessmentRequest.getStatus() == CompletionStatus.GRADED){
            if (updateUserAssessmentRequest.getUserId() == null) {
                addPropertyViolation(context, "userId", "must not be null");
                return false;
            }

            if (updateUserAssessmentRequest.getDomainScores() == null) {
                addPropertyViolation(context, "domainScore", "must not be null");
                return false;
            }
        }

        return true;
    }
}