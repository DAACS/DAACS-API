package com.daacs.framework.validation.child.base

import com.daacs.framework.validation.AbstractValidator
import com.daacs.framework.validation.child.ChildValidator
import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.AssessmentType
import com.daacs.model.assessment.ItemGroupAssessment
import com.daacs.model.item.Item
import com.daacs.model.item.ItemGroup

import javax.validation.ConstraintValidatorContext
import java.text.MessageFormat
/**
 * Created by chostetter on 1/30/17.
 */
public class ItemGroupValidator extends AbstractValidator implements ChildValidator<Assessment> {

    @Override
    public boolean isValid(Assessment assessment, ConstraintValidatorContext context) {

        switch(assessment.getAssessmentType()){

            case AssessmentType.LIKERT:
                return isValidLikertItemGroups(((ItemGroupAssessment) assessment).getItemGroups(), context);

            case AssessmentType.MULTIPLE_CHOICE:
            case AssessmentType.CAT:
            case AssessmentType.WRITING_PROMPT:
                return true;

            default:
                addPropertyViolation(context, "assessmentType", MessageFormat.format("Invalid assessmentType: {0}", assessment.getAssessmentType()))
                return false;
        }
    }

    static boolean isValidLikertItemGroups(List<ItemGroup> itemGroups, ConstraintValidatorContext context){
        boolean isValid = true;
        itemGroups.eachWithIndex{ ItemGroup itemGroup, int i ->
            List<Item> items = itemGroup.getItems();
            List<String> answerOrder = items.size() > 0? items.get(0).getPossibleItemAnswers().collect{ it.content } : []

            items.eachWithIndex{ Item item, int j ->
                if(item.getPossibleItemAnswers().collect{ it.content } != answerOrder){
                    addPropertyViolation(context, "itemGroups[" + i + "]", "All possibleItemAnswers must be the same and in the same order across all items in the ItemGroup");
                    isValid = false;
                }
            }
        }

        return isValid;
    }
}
