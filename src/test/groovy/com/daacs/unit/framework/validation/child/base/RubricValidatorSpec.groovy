package com.daacs.unit.framework.validation.child.base

import com.daacs.framework.validation.child.base.RubricValidator
import com.daacs.model.assessment.*
import com.daacs.model.assessment.user.CompletionScore
import com.daacs.model.item.CATItemGroup
import com.daacs.model.item.Difficulty
import com.daacs.model.item.Item
import com.daacs.model.item.ItemAnswer
import com.daacs.unit.framework.validation.child.ValidatorSpec
import com.google.common.collect.Range
/**
 * Created by chostetter on 8/22/16.
 */
class RubricValidatorSpec extends ValidatorSpec {
    RubricValidator validator;


    CATAssessment catAssessment
    CATAssessment catAssessmentNestedDomains
    WritingAssessment writingAssessment

    def setup(){
        setupContext()

        validator = new RubricValidator()

        catAssessment = new CATAssessment(
                scoringType: ScoringType.AVERAGE,
                itemGroups: [
                        new CATItemGroup(difficulty: Difficulty.MEDIUM, items: [
                                new Item(question: "hi?", domainId: "domain-1", possibleItemAnswers: [ new ItemAnswer(score: 0), new ItemAnswer(score: 1) ]),
                                new Item(question: "hey?", domainId: "domain-2", possibleItemAnswers: [ new ItemAnswer(score: 0), new ItemAnswer(score: 1) ])
                        ])
                ],
                domains: [
                        new ScoringDomain(
                                id: "domain-1",
                                rubric: new Rubric(
                                        completionScoreMap: [
                                                (CompletionScore.LOW): Range.closedOpen(new Double(0.0), new Double(0.25)),
                                                (CompletionScore.MEDIUM): Range.closedOpen(new Double(0.25), new Double(0.5)),
                                                (CompletionScore.HIGH): Range.closed(new Double(0.5), new Double(1.0))

                                        ],
                                        supplementTable: [
                                                new SupplementTableRow(completionScore: CompletionScore.LOW),
                                                new SupplementTableRow(completionScore: CompletionScore.MEDIUM),
                                                new SupplementTableRow(completionScore: CompletionScore.HIGH)
                                        ]
                                )
                        )
                ]
        )

        catAssessmentNestedDomains = new CATAssessment(
                scoringType: ScoringType.SUM,
                itemGroups: [
                        new CATItemGroup(difficulty: Difficulty.MEDIUM, items: [
                                new Item(question: "hi?", domainId: "domain-1", possibleItemAnswers: [ new ItemAnswer(score: 0), new ItemAnswer(score: 1) ]),
                                new Item(question: "hey?", domainId: "domain-2", possibleItemAnswers: [ new ItemAnswer(score: 0), new ItemAnswer(score: 1) ]),
                                new Item(question: "hey?", domainId: "domain-3", possibleItemAnswers: [ new ItemAnswer(score: 0), new ItemAnswer(score: 1) ]),
                                new Item(question: "hey?", domainId: "domain-3", possibleItemAnswers: [ new ItemAnswer(score: 0), new ItemAnswer(score: 1) ]),
                                new Item(question: "hey?", domainId: "domain-3", possibleItemAnswers: [ new ItemAnswer(score: 0), new ItemAnswer(score: 1) ]),
                                new Item(question: "hey?", domainId: "domain-4", possibleItemAnswers: [ new ItemAnswer(score: 0), new ItemAnswer(score: 1) ]),
                                new Item(question: "hey?", domainId: "domain-4", possibleItemAnswers: [ new ItemAnswer(score: 0), new ItemAnswer(score: 1) ]),
                                new Item(question: "hey?", domainId: "domain-5", possibleItemAnswers: [ new ItemAnswer(score: 0), new ItemAnswer(score: 1) ])
                        ])
                ],
                domains: [
                        new ScoringDomain(
                                id: "domain-1",
                                rubric: new Rubric(
                                        completionScoreMap: [
                                                (CompletionScore.LOW): Range.closedOpen(0.0d, 1.0d),
                                                (CompletionScore.MEDIUM): Range.closedOpen(1.0d, 2.0d),
                                                (CompletionScore.HIGH): Range.closed(2.0d, 7.0d)
                                        ],
                                        supplementTable: [
                                                new SupplementTableRow(completionScore: CompletionScore.LOW),
                                                new SupplementTableRow(completionScore: CompletionScore.MEDIUM),
                                                new SupplementTableRow(completionScore: CompletionScore.HIGH)
                                        ]
                                ),
                                subDomains: [
                                        new ScoringDomain(
                                                id: "domain-3",
                                                rubric: new Rubric(
                                                        completionScoreMap: [
                                                                (CompletionScore.HIGH): Range.closed(0.0d, 4.0d)
                                                        ],
                                                        supplementTable: [
                                                                new SupplementTableRow(completionScore: CompletionScore.HIGH)
                                                        ]
                                                ),
                                                subDomains: [
                                                        new ScoringDomain(
                                                                id: "domain-5",
                                                                rubric: new Rubric(
                                                                        completionScoreMap: [
                                                                                (CompletionScore.HIGH): Range.closed(0.0d, 1.0d)
                                                                        ],
                                                                        supplementTable: [
                                                                                new SupplementTableRow(completionScore: CompletionScore.HIGH)
                                                                        ]
                                                                )
                                                        )
                                                ]
                                        ),
                                        new ScoringDomain(
                                                id: "domain-4",
                                                rubric: new Rubric(
                                                        completionScoreMap: [
                                                                (CompletionScore.HIGH): Range.closed(0.0d, 2.0d)
                                                        ],
                                                        supplementTable: [
                                                                new SupplementTableRow(completionScore: CompletionScore.HIGH)
                                                        ]
                                                )
                                        )
                                ]
                        ),
                ]
        )

        writingAssessment = new WritingAssessment(
                domains: [
                        new ScoringDomain(
                                id: "domain-1",
                                rubric: new Rubric(
                                        completionScoreMap: Collections.EMPTY_MAP,
                                        supplementTable: [new SupplementTableRow()]
                                )
                        )
                ]
        )
    }


    def "isValid (WritingAssessment): success for domain"(){
        when:
        boolean isValid = validator.isValid(writingAssessment, context, writingAssessment.getDomains().get(0))

        then:
        isValid
    }

    def "isValid (WritingAssessment): failure for domain: invalid completionScoreMap"(){
        setup:
        writingAssessment.getDomains().find{ it.id == "domain-1" }.getRubric()
                .setCompletionScoreMap([(CompletionScore.HIGH): Range.atLeast(5)])

        when:
        boolean isValid = validator.isValid(writingAssessment, context, writingAssessment.getDomains().get(0))

        then:
        !isValid
    }

    def "isValid (WritingAssessment): failure for domain: invalid supplementTable"(){
        setup:
        writingAssessment.getDomains().find{ it.id == "domain-1" }.getRubric()
                .setSupplementTable([])

        when:
        boolean isValid = validator.isValid(writingAssessment, context, writingAssessment.getDomains().get(0))

        then:
        !isValid
    }

    def "isValid (WritingAssessment): failure for domain: no domain"(){
        when:
        boolean isValid = validator.isValid(writingAssessment, context, new ScoringDomain(id: "domain-9999", rubric: new Rubric(
                completionScoreMap: Collections.EMPTY_MAP,
                supplementTable: [new SupplementTableRow()]
        )))

        then:
        !isValid
    }

    def "isValid (ItemGroupAssessment): success for domain"(){
        when:
        boolean isValid = validator.isValid(catAssessment, context, catAssessment.getDomains().get(0))

        then:
        isValid
    }

    def "isValid (ItemGroupAssessment): failure for domain: bad domain"(){
        when:
        boolean isValid = validator.isValid(catAssessment, context, new ScoringDomain(id: "domain-999999", rubric: new Rubric(
                completionScoreMap: Collections.EMPTY_MAP,
                supplementTable: [new SupplementTableRow()]
        )))

        then:
        !isValid
    }

    def "isValid (ItemGroupAssessment): failure for domain: bad supplementTable"(){
        setup:
        catAssessment.getDomains().find{ it.id == "domain-1" }.getRubric()
                .setSupplementTable([])

        when:
        boolean isValid = validator.isValid(catAssessment, context, catAssessment.getDomains().get(0))

        then:
        !isValid
    }

    def "isValid (ItemGroupAssessment): failure for domain: bad completionScoreMap"(){
        setup:
        catAssessment.getDomains().find{ it.id == "domain-1" }.getRubric()
                .getCompletionScoreMap().put(CompletionScore.LOW, Range.closedOpen(new Double(-1.0), new Double(0.25)))

        when:
        boolean isValid = validator.isValid(catAssessment, context, catAssessment.getDomains().get(0))

        then:
        !isValid
    }

    def "isValid (ItemGroupAssessment): failure for domain: bad itemGroups"(){
        setup:
        catAssessment.setItemGroups([])

        when:
        boolean isValid = validator.isValid(catAssessment, context, catAssessment.getDomains().get(0))

        then:
        !isValid
    }

    def "isValid (ItemGroupAssessment): success for domain with subdomain"(){
        when:
        boolean isValid = validator.isValid(catAssessmentNestedDomains, context, catAssessmentNestedDomains.getDomains().get(0))

        then:
        isValid
    }

    def "isValid (ItemGroupAssessment): success for subdomain"(){
        when:
        boolean isValid = validator.isValid(catAssessmentNestedDomains, context, catAssessmentNestedDomains.getDomains().get(0).getSubDomains().get(0))

        then:
        isValid
    }

    def "isValid (ItemGroupAssessment): success for subdomain #2"(){
        when:
        boolean isValid = validator.isValid(catAssessmentNestedDomains, context, catAssessmentNestedDomains.getDomains().get(0).getSubDomains().get(1))

        then:
        isValid
    }

    def "isValid (ItemGroupAssessment): fails for domain with subdomain"(){
        setup:
        catAssessmentNestedDomains.getDomains().get(0).getRubric().setCompletionScoreMap([
                (CompletionScore.LOW): Range.closedOpen(0.0d, 1.0d),
                (CompletionScore.MEDIUM): Range.closedOpen(1.0d, 2.0d),
                (CompletionScore.HIGH): Range.closed(2.0d, 3.0d)
        ])

        when:
        boolean isValid = validator.isValid(catAssessmentNestedDomains, context, catAssessmentNestedDomains.getDomains().get(0))

        then:
        !isValid
    }

    def "isValid (ItemGroupAssessment): fails for subdomain"(){
        setup:
        catAssessmentNestedDomains.getDomains().get(0).getSubDomains().get(0).getRubric().setCompletionScoreMap([
                (CompletionScore.HIGH): Range.closed(2.0d, 3.0d)
        ])

        when:
        boolean isValid = validator.isValid(catAssessmentNestedDomains, context, catAssessmentNestedDomains.getDomains().get(0).getSubDomains().get(0))

        then:
        !isValid
    }

    def "isValid (ItemGroupAssessment): fails for subdomain #2"(){
        setup:
        catAssessmentNestedDomains.getDomains().get(0).getSubDomains().get(1).getRubric().setCompletionScoreMap([
                (CompletionScore.HIGH): Range.closed(2.0d, 3.0d)
        ])

        when:
        boolean isValid = validator.isValid(catAssessmentNestedDomains, context, catAssessmentNestedDomains.getDomains().get(0).getSubDomains().get(1))

        then:
        !isValid
    }

    def "isValid (ItemGroupAssessment): success for subsubdomain "(){
        when:
        boolean isValid = validator.isValid(catAssessmentNestedDomains, context, catAssessmentNestedDomains.getDomains().get(0).getSubDomains().get(0).getSubDomains().get(0))

        then:
        isValid
    }

    def "isValid (ItemGroupAssessment): fails if rubric is null and ScoreIsSubDomainAverage is false"(){
        setup:
        ((ScoringDomain)catAssessmentNestedDomains.getDomains().get(0)).setScoreIsSubDomainAverage(false)
        ((ScoringDomain)catAssessmentNestedDomains.getDomains().get(0)).setRubric(null)

        when:
        boolean isValid = validator.isValid(catAssessmentNestedDomains, context, catAssessmentNestedDomains.getDomains().get(0))

        then:
        !isValid
    }

    def "isValid (WritingAssessment): fails if rubric is null and ScoreIsSubDomainAverage is false"(){
        setup:
        ((ScoringDomain)writingAssessment.getDomains().get(0)).setScoreIsSubDomainAverage(false)
        ((ScoringDomain)writingAssessment.getDomains().get(0)).setRubric(null)

        when:
        boolean isValid = validator.isValid(writingAssessment, context, writingAssessment.getDomains().get(0))

        then:
        !isValid
    }
}