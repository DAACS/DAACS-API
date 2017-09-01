package com.daacs.framework.validation.child.cat

import com.daacs.framework.validation.AbstractValidator
import com.daacs.framework.validation.child.ChildValidator
import com.daacs.model.assessment.CATAssessment
import com.daacs.model.assessment.Domain
import com.daacs.model.assessment.ScoringDomain
import com.daacs.model.item.CATItemGroup
import com.daacs.model.item.Difficulty

import javax.validation.ConstraintValidatorContext
import java.text.MessageFormat

/**
 * Created by chostetter on 8/19/16.
 */
class CATItemGroupValidator extends AbstractValidator implements ChildValidator<CATAssessment> {

    @Override
    boolean isValid(CATAssessment assessment, ConstraintValidatorContext context) {

        //check to make sure we have enough itemGroups to cover these
        //we'll need maxTakenGroups - 1 for all groups, +1 for startingDifficulty group

        boolean isValid = true;
        Difficulty.values().each { difficulty ->
            List<CATItemGroup> itemGroups = assessment.getItemGroups().findAll{ it.getDifficulty() == difficulty }

            boolean isStartingDifficulty = difficulty == assessment.getStartingDifficulty();

            int numRequired = assessment.getMaxTakenGroups() - 1 + (isStartingDifficulty? 1 : 0)

            if(itemGroups.size() < numRequired){
                addPropertyViolation(context,
                        "itemGroups",
                        MessageFormat.format(
                                "Invalid itemGroups, expecting {0} itemGroups w/difficulty {1} but only found {2}",
                                numRequired,
                                difficulty,
                                itemGroups.size()));

                isValid = false;
            }
        }

        //check to make sure all the domains are defined
        Set<String> domainIds = [];
        assessment.getItemGroups().each{ itemGroup ->
            domainIds.addAll(itemGroup.getItems().collect{ it.getDomainId() });
        }

        Set<String> definedDomainIds = getDomainIds(assessment.getDomains());
        domainIds.removeAll(definedDomainIds);

        if(domainIds.size() > 0){
            addPropertyViolation(context,
                    "itemGroups",
                    MessageFormat.format(
                            "no definition for domains w/id {0}",
                            domainIds));

            isValid = false;
        }

        return isValid;
    }

    List<String> getDomainIds(List<Domain> domains){
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
