package com.daacs.unit.framework.validation.child.base

import com.daacs.framework.validation.child.base.DomainsValidator
import com.daacs.framework.validation.child.base.RubricValidator
import com.daacs.model.assessment.*
import com.daacs.unit.framework.validation.child.ValidatorSpec

/**
 * Created by chostetter on 8/22/16.
 */
class DomainsValidatorSpec extends ValidatorSpec {
    DomainsValidator validator;
    RubricValidator rubricValidator;

    CATAssessment catAssessment
    WritingAssessment writingAssessment

    def setup(){
        setupContext()

        rubricValidator = GroovySpy(RubricValidator, global: true)

        validator = new DomainsValidator()

        catAssessment = new CATAssessment(
                assessmentType: AssessmentType.CAT,
                domains: [new ScoringDomain(id: "domain-1", subDomains: [new ScoringDomain(id: "domain-3")]), new ScoringDomain(id: "domain-2")]
        )

        writingAssessment = new WritingAssessment(
                assessmentType: AssessmentType.WRITING_PROMPT,
                domains: [new ScoringDomain(id: "domain-1")]
        )
    }

    def "rubricValidator gets called (CATAssessment): success"(){
        when:
        boolean isValid = validator.isValid((Assessment) catAssessment, context)

        then:
        1 * RubricValidator.isValid((ItemGroupAssessment) catAssessment, context, catAssessment.getDomains().get(0)) >> true
        1 * RubricValidator.isValid((ItemGroupAssessment) catAssessment, context, catAssessment.getDomains().get(1)) >> true
        1 * RubricValidator.isValid((ItemGroupAssessment) catAssessment, context, catAssessment.getDomains().get(0).getSubDomains().get(0)) >> true

        then:
        isValid
    }

    def "rubricValidator gets called (WritingAssessment): success"(){
        when:
        boolean isValid = validator.isValid((Assessment) writingAssessment, context)

        then:
        1 * RubricValidator.isValid(writingAssessment, context, writingAssessment.getDomains().get(0)) >> true

        then:
        isValid
    }

    def "rubricValidator gets called (CATAssessment): false"(){
        when:
        boolean isValid = validator.isValid((Assessment) catAssessment, context)

        then:
        1 * RubricValidator.isValid((ItemGroupAssessment) catAssessment, context, catAssessment.getDomains().get(0)) >> false
        1 * RubricValidator.isValid((ItemGroupAssessment) catAssessment, context, catAssessment.getDomains().get(1)) >> true
        1 * RubricValidator.isValid((ItemGroupAssessment) catAssessment, context, catAssessment.getDomains().get(0).getSubDomains().get(0)) >> true

        then:
        !isValid
    }

    def "rubricValidator gets called (WritingAssessment): false"(){
        when:
        boolean isValid = validator.isValid((Assessment) writingAssessment, context)

        then:
        1 * RubricValidator.isValid(writingAssessment, context, writingAssessment.getDomains().get(0)) >> false

        then:
        !isValid
    }

    def "finds duplicate domainIds on catAssessment"(){
        setup:
        RubricValidator.isValid((ItemGroupAssessment) catAssessment, context, _) >> true

        catAssessment.setDomains([
                new ScoringDomain(id: "domain-1", subDomains: [
                        new ScoringDomain(id: "domain-3")
                ]),
                new ScoringDomain(id: "domain-4", subDomains: [
                        new ScoringDomain(id: "domain-3")
                ]),
                new ScoringDomain(id: "domain-2")])

        when:
        boolean isValid = validator.isValid((Assessment) catAssessment, context)

        then:
        !isValid
    }

    def "finds duplicate domainIds on writingAssessment"(){
        setup:
        RubricValidator.isValid(writingAssessment, context, _) >> true

        writingAssessment.setDomains([
                new ScoringDomain(id: "domain-1", subDomains: [
                        new ScoringDomain(id: "domain-3")
                ]),
                new ScoringDomain(id: "domain-4", subDomains: [
                        new ScoringDomain(id: "domain-3")
                ]),
                new ScoringDomain(id: "domain-2")])

        when:
        boolean isValid = validator.isValid((Assessment) writingAssessment, context)

        then:
        !isValid
    }
}