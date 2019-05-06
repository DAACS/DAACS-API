package com.daacs.unit.framework.validation.child.base

import com.daacs.framework.validation.child.base.OverallRubricValidator
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
class OverallRubricValidatorSpec extends ValidatorSpec {
    OverallRubricValidator validator;

    CATAssessment catAssessment
    WritingAssessment writingAssessment

    def setup(){
        setupContext()

        validator = new OverallRubricValidator()

        catAssessment = new CATAssessment(
                scoringType: ScoringType.AVERAGE,
                assessmentType: AssessmentType.CAT,
                overallRubric: new Rubric(
                        completionScoreMap: [
                                (CompletionScore.LOW)   : Range.closedOpen(new Double(0.0), new Double(0.25)),
                                (CompletionScore.MEDIUM): Range.closedOpen(new Double(0.25), new Double(0.5)),
                                (CompletionScore.HIGH)  : Range.closed(new Double(0.5), new Double(1.0))

                        ],
                        supplementTable: [
                                new SupplementTableRow(completionScore: CompletionScore.LOW),
                                new SupplementTableRow(completionScore: CompletionScore.MEDIUM),
                                new SupplementTableRow(completionScore: CompletionScore.HIGH)
                        ]
                ),
                itemGroups: [
                        new CATItemGroup(difficulty: Difficulty.MEDIUM, items: [
                                new Item(question: "hi?", domainId: "domain-1", possibleItemAnswers: [new ItemAnswer(score: 0), new ItemAnswer(score: 1) ]),
                                new Item(question: "hey?", domainId: "domain-2", possibleItemAnswers: [ new ItemAnswer(score: 0), new ItemAnswer(score: 1) ])
                        ])
                ]
        )

        writingAssessment = new WritingAssessment(
                scoringType: ScoringType.MANUAL,
                assessmentType: AssessmentType.WRITING_PROMPT,
                overallRubric: new Rubric(
                        completionScoreMap: Collections.EMPTY_MAP,
                        supplementTable: [new SupplementTableRow()]
                )
        )
    }

    def "isValid (WritingAssessment): success for overall"(){
        when:
        boolean isValid = validator.isValid((Assessment) writingAssessment, context)

        then:
        isValid
    }

    def "isValid (WritingAssessment): failure for overall: invalid CompletionScoreMap"(){
        setup:
        writingAssessment.getOverallRubric().setCompletionScoreMap([(CompletionScore.HIGH): Range.atLeast(5)])

        when:
        boolean isValid = validator.isValid((Assessment) writingAssessment, context)

        then:
        !isValid
    }

    def "isValid (WritingAssessment): failure for overall: invalid CompletionScoreMap, empty range"(){
        setup:
        writingAssessment.getOverallRubric().setCompletionScoreMap([(CompletionScore.HIGH): null])

        when:
        boolean isValid = validator.isValid((Assessment) writingAssessment, context)

        then:
        !isValid
    }

    def "isValid (WritingAssessment): failure for overall: invalid supplementTable"(){
        setup:
        writingAssessment.getOverallRubric().setSupplementTable([])

        when:
        boolean isValid = validator.isValid((Assessment) writingAssessment, context)

        then:
        !isValid
    }

    def "isValid (ItemGroupAssessment): success for overall"(){
        when:
        boolean isValid = validator.isValid((Assessment) catAssessment, context)

        then:
        isValid
    }

    def "isValid (ItemGroupAssessment): failure for overall: bad supplementTable"(){
        setup:
        catAssessment.getOverallRubric().setSupplementTable([])

        when:
        boolean isValid = validator.isValid((Assessment) catAssessment, context)

        then:
        !isValid
    }

    def "isValid (ItemGroupAssessment): failure for overall: bad scoringType"(){
        setup:
        catAssessment.setScoringType(ScoringType.SUM)

        when:
        boolean isValid = validator.isValid((Assessment) catAssessment, context)

        then:
        !isValid
    }

    def "isValid (ItemGroupAssessment): failure for overall: bad completionScoreMap"(){
        setup:
        catAssessment.getOverallRubric().getCompletionScoreMap().put(CompletionScore.LOW, Range.closedOpen(new Double(-1.0), new Double(0.25)))

        when:
        boolean isValid = validator.isValid((Assessment) catAssessment, context)

        then:
        !isValid
    }

    def "isValid (ItemGroupAssessment): failure for overall: bad itemGroups"(){
        setup:
        catAssessment.setItemGroups([])

        when:
        boolean isValid = validator.isValid((Assessment) catAssessment, context)

        then:
        !isValid
    }

}