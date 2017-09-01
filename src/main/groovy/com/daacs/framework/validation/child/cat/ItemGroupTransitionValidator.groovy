package com.daacs.framework.validation.child.cat

import com.daacs.framework.validation.AbstractValidator
import com.daacs.framework.validation.child.ChildValidator
import com.daacs.model.assessment.CATAssessment
import com.daacs.model.item.ItemGroupTransition
import com.google.common.collect.Range

import javax.validation.ConstraintValidatorContext
/**
 * Created by chostetter on 8/19/16.
 */
class ItemGroupTransitionValidator extends AbstractValidator implements ChildValidator<CATAssessment> {

    @Override
    boolean isValid(CATAssessment assessment, ConstraintValidatorContext context) {
        List<ItemGroupTransition> itemGroupTransitions = assessment.getItemGroupTransitions()

        if(itemGroupTransitions.size() == 0){
            addPropertyViolation(context,
                    "itemGroupTransitions",
                    "itemGroupTransitions must contain at least one transition");

            return false;
        }

        boolean isValid = true;

        for(int i = 0; i < itemGroupTransitions.size(); i++){
            ItemGroupTransition itemGroupTransition = itemGroupTransitions.get(i)

            Integer start = Integer.MIN_VALUE;

            List<Range<Integer>> ranges = itemGroupTransition.getTransitionMap().entrySet().collect{ it.getValue() }

            for(int j = 0; j < ranges.size(); j++){
                Range<Integer> range = ranges.get(j);
                if(ranges.size() == 1 && range.hasUpperBound()){
                    break;
                }

                if(!range.hasLowerBound() || range.lowerEndpoint() == start){

                    if(range.hasUpperBound()){
                        start = range.upperEndpoint() + 1;
                    }

                    ranges.remove(j);
                    j = -1; //start over
                }
            }

            if(ranges.size() > 0){
                addPropertyViolation(context,
                        "itemGroupTransitions[" + i + "].transitionMap",
                        "has a gap in its coverage");


                isValid = false;
            }
        }

        return isValid;
    }

}
