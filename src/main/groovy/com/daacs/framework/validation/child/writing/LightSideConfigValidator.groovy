package com.daacs.framework.validation.child.writing

import com.daacs.framework.validation.AbstractValidator
import com.daacs.framework.validation.child.ChildValidator
import com.daacs.model.assessment.Domain
import com.daacs.model.assessment.ScoringDomain
import com.daacs.model.assessment.ScoringType
import com.daacs.model.assessment.WritingAssessment

import javax.validation.ConstraintValidatorContext

/**
 * Created by chostetter on 8/19/16.
 */
class LightSideConfigValidator extends AbstractValidator implements ChildValidator<WritingAssessment> {

    @Override
    boolean isValid(WritingAssessment assessment, ConstraintValidatorContext context) {
        if (assessment.getScoringType() != ScoringType.LIGHTSIDE) return true;

        if (assessment.getLightSideConfig() == null) {
            addPropertyViolation(context, "lightSideConfig", "must not be null");
            return false;
        }

        //get all scoring domains that aren't configured for subDomain average calculation
        Set<String> domainIds = new HashSet<>()
        for (Domain domain : assessment.getDomains()) {
            if (domain instanceof ScoringDomain) {
                if (!domain.scoreIsSubDomainAverage) {
                    domainIds.add(domain.id)
                }
                for (Domain subDomain in domain.getSubDomains()) {
                    domainIds.add(subDomain.id)
                }
            }
        }

        Set<String> lightSideDomainIds = assessment.getLightSideConfig().getDomainModels().keySet();
        if (!lightSideDomainIds.containsAll(domainIds)) {
            addPropertyViolation(context, "lightSideConfig.domainModels", "missing model configuration for some domains");
            return false;

        }

        return true;
    }
}
