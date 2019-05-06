package com.daacs.framework.validation.child.base

import com.daacs.framework.validation.AbstractValidator
import com.daacs.framework.validation.child.ChildValidator
import com.daacs.model.assessment.*

import javax.validation.ConstraintValidatorContext
import java.text.MessageFormat
/**
 * Created by chostetter on 8/19/16.
 */
public class DomainsValidator extends AbstractValidator implements ChildValidator<Assessment> {

    @Override
    public boolean isValid(Assessment assessment, ConstraintValidatorContext context) {

        switch(assessment.getAssessmentType()){
            case AssessmentType.WRITING_PROMPT:

                if (assessment instanceof WritingAssessment) {
                    return isValid((WritingAssessment) assessment, context);
                } else {
                    return buildAndReturnAssessmentTypePropertyViolation(context, assessment.getAssessmentType())
                }

            case AssessmentType.LIKERT:
            case AssessmentType.MULTIPLE_CHOICE:
            case AssessmentType.CAT:

                if (assessment instanceof ItemGroupAssessment) {
                    return isValid((ItemGroupAssessment) assessment, context);
                } else {
                    return buildAndReturnAssessmentTypePropertyViolation(context, assessment.getAssessmentType())
                }

            default:
                return buildAndReturnAssessmentTypePropertyViolation(context, assessment.getAssessmentType())
        }

    }

    boolean isValid(WritingAssessment assessment, ConstraintValidatorContext context){
        List<Domain> invalidDomains = getInvalidDomains(assessment, context, assessment.getDomains());
        List<String> duplicateDomainIds = getDuplicateDomainIds(assessment.getDomains());
        if(duplicateDomainIds.size() > 0){
            addPropertyViolation(context,
                    "domains",
                    "domain IDs must be unique. Found duplicates: " + String.join(", ", duplicateDomainIds));
        }

        return invalidDomains.size() == 0 && duplicateDomainIds.size() == 0
    }

    boolean isValid(ItemGroupAssessment assessment, ConstraintValidatorContext context){
        List<Domain> invalidDomains = getInvalidDomains(assessment, context, assessment.getDomains());
        List<String> duplicateDomainIds = getDuplicateDomainIds(assessment.getDomains());
        if(duplicateDomainIds.size() > 0){
            addPropertyViolation(context,
                    "domains",
                    "domain IDs must be unique. Found duplicates: " + String.join(", ", duplicateDomainIds));
        }

        return invalidDomains.size() == 0 && duplicateDomainIds.size() == 0
    }

    public <T extends Assessment> List<Domain> getInvalidDomains(T assessment, ConstraintValidatorContext context, List<Domain> domains){
        List<Domain> invalidDomains = [];
        for(Domain domain : domains){
            if(domain instanceof ScoringDomain){
                if(domain.subDomains != null){
                    invalidDomains.addAll(getInvalidDomains(assessment, context, domain.subDomains));
                }

                if(!RubricValidator.isValid(assessment, context, domain)){
                    invalidDomains.add(domain);
                }
            }
        }

        return invalidDomains;
    }

    public List<String> getDuplicateDomainIds(List<Domain> domains){
        List<String> allDomainIds = getDomainIds(domains);

        List<String> duplicateDomainIds = [];
        for(String domainId : allDomainIds) {
            if(Collections.frequency(allDomainIds, domainId) > 1) {
                duplicateDomainIds.add(domainId);
            }
        }


        return duplicateDomainIds;
    }

    private List<String> getDomainIds(List<Domain> domains){
        List<String> domainIds = [];
        for(Domain domain: domains){
            if(domain instanceof ScoringDomain && domain.subDomains != null){
                domainIds.addAll(getDomainIds(domain.subDomains));
            }

            domainIds.add(domain.id);
        }

        return domainIds;
    }
}