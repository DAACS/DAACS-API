package com.daacs.unit.framework.validation.child.cat

import com.daacs.framework.validation.child.cat.CATItemGroupValidator
import com.daacs.model.assessment.CATAssessment
import com.daacs.model.assessment.ScoringDomain
import com.daacs.model.item.CATItemGroup
import com.daacs.model.item.Difficulty
import com.daacs.model.item.Item
import com.daacs.unit.framework.validation.child.ValidatorSpec
/**
 * Created by chostetter on 8/22/16.
 */
class CATItemGroupValidatorSpec extends ValidatorSpec {
    CATItemGroupValidator validator;
    CATAssessment catAssessment;

    def setup(){
        setupContext()
        validator = new CATItemGroupValidator()

        catAssessment = new CATAssessment(
                maxTakenGroups: 2,
                startingDifficulty: Difficulty.MEDIUM,
                itemGroups: [
                        new CATItemGroup(difficulty: Difficulty.MEDIUM, items: [ new Item(domainId: "domain-1") ]),
                        new CATItemGroup(difficulty: Difficulty.MEDIUM, items: [ new Item(domainId: "domain-1") ]),
                        new CATItemGroup(difficulty: Difficulty.EASY, items: [ new Item(domainId: "domain-2") ]),
                        new CATItemGroup(difficulty: Difficulty.HARD, items: [ new Item(domainId: "domain-2") ])
                ],
                domains: [new ScoringDomain(id: "domain-1"), new ScoringDomain(id: "domain-2") ]
        )
    }

    def "isValid: passes"(){
        when:
        boolean isValid = validator.isValid(catAssessment, context)

        then:
        isValid
    }

    def "isValid: fails - maxTakenGroups says we need at least two of each group"(){
        setup:
        catAssessment.maxTakenGroups = 3

        when:
        boolean isValid = validator.isValid(catAssessment, context)

        then:
        !isValid
    }

    def "isValid: fails with not enough for starting difficulty group"(){
        setup:
        catAssessment.itemGroups = [
                new CATItemGroup(difficulty: Difficulty.MEDIUM),
                new CATItemGroup(difficulty: Difficulty.EASY),
                new CATItemGroup(difficulty: Difficulty.HARD)
        ]

        when:
        boolean isValid = validator.isValid(catAssessment, context)

        then:
        !isValid
    }

    def "isValid: fails when domain is not defined"(){
        setup:
        catAssessment.domains = []

        when:
        boolean isValid = validator.isValid(catAssessment, context)

        then:
        !isValid
    }
}