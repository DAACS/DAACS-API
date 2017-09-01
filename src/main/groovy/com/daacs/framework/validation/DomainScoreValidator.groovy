package com.daacs.framework.validation

import com.daacs.framework.validation.annotations.ValidDomainScore
import com.daacs.model.assessment.user.DomainScore

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
/**
 * Created by chostetter on 3/30/17.
 */

class DomainScoreValidator extends AbstractValidator implements ConstraintValidator<ValidDomainScore, DomainScore> {

    @Override
    public void initialize(ValidDomainScore constraintAnnotation) {}

    @Override
    public boolean isValid(DomainScore domainScore, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        if (domainScore.rubricScore == null && (domainScore.subDomainScores == null || domainScore.subDomainScores.size() == 0)) {
            addPropertyViolation(context, "rubricScore", "cannot be null if zero subDomainScores defined");
            return false;
        }

        return true;
    }
}