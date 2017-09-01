package com.daacs.unit.framework.validation.child.base

import com.daacs.framework.validation.child.base.ItemGroupValidator
import com.daacs.model.assessment.ItemGroupAssessment
import com.daacs.model.item.Item
import com.daacs.model.item.ItemAnswer
import com.daacs.model.item.ItemGroup
import com.daacs.unit.framework.validation.child.ValidatorSpec

import static com.daacs.model.assessment.AssessmentType.LIKERT
/**
 * Created by chostetter on 1/30/17.
 */
class ItemGroupValidatorSpec extends ValidatorSpec {
    ItemGroupValidator validator;
    ItemGroupAssessment assessment;
    List<ItemGroup> itemGroups;
    def setup(){
        setupContext()

        itemGroups = [
                new ItemGroup(items: [
                        new Item(possibleItemAnswers: [
                                new ItemAnswer(content: "Strongly Agree"),
                                new ItemAnswer(content: "Agree"),
                                new ItemAnswer(content: "Neither Agree nor Disagree"),
                                new ItemAnswer(content: "Disagree"),
                                new ItemAnswer(content: "Strongly Disagree")
                        ]),
                        new Item(possibleItemAnswers: [
                                new ItemAnswer(content: "Strongly Agree"),
                                new ItemAnswer(content: "Agree"),
                                new ItemAnswer(content: "Neither Agree nor Disagree"),
                                new ItemAnswer(content: "Disagree"),
                                new ItemAnswer(content: "Strongly Disagree")
                        ])
                ]),
                new ItemGroup(items: [
                        new Item(possibleItemAnswers: [
                                new ItemAnswer(content: "Strongly Agree"),
                                new ItemAnswer(content: "Agree"),
                                new ItemAnswer(content: "Neither Agree nor Disagree"),
                                new ItemAnswer(content: "Disagree"),
                                new ItemAnswer(content: "Strongly Disagree")
                        ]),
                        new Item(possibleItemAnswers: [
                                new ItemAnswer(content: "Strongly Agree"),
                                new ItemAnswer(content: "Agree"),
                                new ItemAnswer(content: "Neither Agree nor Disagree"),
                                new ItemAnswer(content: "Disagree"),
                                new ItemAnswer(content: "Strongly Disagree")
                        ])
                ])
        ]

        validator = new ItemGroupValidator()

        assessment = Mock(ItemGroupAssessment)
        assessment.getAssessmentType() >> LIKERT
        assessment.getItemGroups() >> itemGroups

    }

    def "passes validation"(){
        when:
        boolean isValid = validator.isValid(assessment, context)

        then:
        isValid
    }

    def "fails validation"(){
        setup:
        itemGroups.get(0).getItems().add(new Item(possibleItemAnswers: [
                new ItemAnswer(content: "Strongly Disagree"),
                new ItemAnswer(content: "Disagree"),
                new ItemAnswer(content: "Neither Agree nor Disagree"),
                new ItemAnswer(content: "Agree"),
                new ItemAnswer(content: "Strongly Agree")
        ]))

        when:
        boolean isValid = validator.isValid(assessment, context)

        then:
        !isValid
    }

}