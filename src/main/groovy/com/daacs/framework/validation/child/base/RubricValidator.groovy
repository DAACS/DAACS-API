package com.daacs.framework.validation.child.base

import com.daacs.framework.validation.AbstractValidator
import com.daacs.model.assessment.*
import com.daacs.model.assessment.user.CompletionScore
import com.daacs.model.item.ItemGroup
import com.google.common.collect.Range

import javax.validation.ConstraintValidatorContext
import java.text.MessageFormat

/**
 * Created by chostetter on 8/19/16.
 */
public class RubricValidator extends AbstractValidator {

    public static boolean isValid(WritingAssessment assessment, ConstraintValidatorContext context, ScoringDomain domain){

        Rubric rubric = assessment.getOverallRubric();
        String validationKey = "overallRubric";

        if(domain != null){
            String domainValidationKey = getValidationKey(assessment.getDomains(), domain.id);
            if(domainValidationKey == null){
                addPropertyViolation(context,
                        validationKey,
                        "domain does not exist on assessment");

                return false;
            }

            validationKey = domainValidationKey;

            if(domain.getScoreIsSubDomainAverage()){
                return true;
            }

            if(domain.getRubric() == null && !domain.getScoreIsSubDomainAverage()){
                addPropertyViolation(context,
                        validationKey,
                        "domain must contain a rubric if scoreIsSubDomainAverage is false");

                return false;
            }

            rubric = domain.getRubric();
        }

        if(rubric.getCompletionScoreMap() != null && rubric.getCompletionScoreMap().size() > 0){
            addPropertyViolation(context,
                    validationKey,
                    "contains entries in completionScoreMap");

            return false;
        }

        if(rubric.getSupplementTable().size() == 0){
            addPropertyViolation(context,
                    validationKey,
                    "does not contain entries in supplementTable");

            return false;
        }

        return true;
    }

    public static boolean isValid(ItemGroupAssessment assessment, ConstraintValidatorContext context, ScoringDomain domain){

        Rubric rubric = assessment.getOverallRubric();
        String validationKey = "overallRubric";
        List<String> scoringDomainIds;

        if(domain != null){
            String domainValidationKey = getValidationKey(assessment.getDomains(), domain.id);
            if(domainValidationKey == null){
                addPropertyViolation(context,
                        validationKey,
                        "domain does not exist on assessment");

                return false;
            }

            validationKey = domainValidationKey;

            if(domain.getScoreIsSubDomainAverage()){
                return true;
            }

            if(domain.getRubric() == null && !domain.getScoreIsSubDomainAverage()){
                addPropertyViolation(context,
                        validationKey,
                        "domain must contain a rubric if scoreIsSubDomainAverage is false");

                return false;
            }

            rubric = domain.getRubric();
            scoringDomainIds = getScoringDomainIds(domain);
        }

        //ensure completionScoreMap range is from 0 to 1 with no gaps.
        List<Range<Double>> ranges = rubric.getCompletionScoreMap().entrySet().collect{ it.getValue() }

        Double overallLowestScore = 0.0;
        Double overallHighestScore = 0.0;

        int numQuestions = 0;

        assessment.getItemGroups().each { itemGroup ->
            ((ItemGroup) itemGroup).getItems().each{ item ->

                if(domain == null || scoringDomainIds.contains(item.getDomainId())){
                    Integer lowestAnswerScore = 0;
                    Integer highestAnswerScore = 0;
                    item.getPossibleItemAnswers().each { itemAnswer ->
                        if(itemAnswer.score != null){
                            if(itemAnswer.score < lowestAnswerScore) lowestAnswerScore = itemAnswer.score;
                            if(itemAnswer.score > highestAnswerScore) highestAnswerScore = itemAnswer.score;
                        }
                    }

                    numQuestions++;
                    overallLowestScore += lowestAnswerScore;
                    overallHighestScore += highestAnswerScore;
                }

            }
        }
        Double start;
        Double finish;

        switch(assessment.getScoringType()){
            case ScoringType.AVERAGE:
                start = Math.floor(new Double(overallLowestScore/numQuestions));
                finish = Math.ceil(new Double(overallHighestScore/numQuestions));
                break;

            case ScoringType.SUM:
                start = Math.floor(new Double(overallLowestScore));
                finish = Math.ceil(new Double(overallHighestScore));
                break;

            default:
                addPropertyViolation(context, "scoringType", MessageFormat.format("Incompatible scoringType: {0}", assessment.getScoringType()))
                return false;
        }

        if(start == finish){
            addPropertyViolation(context,
                    "itemGroups",
                    "lowest possible score is same as highest possible score");

            return false;
        }

        for(int i = 0; i < ranges.size(); i++){
            if(ranges.size() == 1 && ranges.get(0).upperEndpoint() != finish){
                break;
            }

            if(ranges.get(i).lowerEndpoint() == start){
                start = ranges.get(i).upperEndpoint();
                ranges.remove(i);
                i = -1; //start over
            }
        }

        if(ranges.size() > 0){
            addPropertyViolation(context,
                    validationKey + ".completionScoreMap",
                    "has a gap in its coverage, is unaligned, or scoringType is incorrect. If this domain contains sub-domains, this domain's scoring must account for it.");

            return false;
        }

        //ensure completionScoreMap has matching entries for supplementTable.
        List<CompletionScore> mapEntries = rubric.getCompletionScoreMap().entrySet().collect{ it.getKey() }
        List<CompletionScore> tableEntries = rubric.getSupplementTable().collect{ it.completionScore }

        if(!tableEntries.containsAll(mapEntries)){
            addPropertyViolation(context,
                    validationKey + ".supplementTable",
                    "does not cover all completion scores");

            return false;
        }

        return true;
    }

    protected static List<String> getScoringDomainIds(ScoringDomain domain){
        List<String> domainIds = new ArrayList<>();
        if(domain.getSubDomains() != null){
            for(Domain subDomain: domain.getSubDomains()){
                if(subDomain instanceof ScoringDomain){
                    domainIds.addAll(getScoringDomainIds(subDomain));
                }
            }
        }

        domainIds.add(domain.getId());
        return domainIds;
    }

    private static List<String> getValidationKeyParts(List<Domain> domains, String domainId){
        List<String> parts = [];

        domains.eachWithIndex { it, index ->
            if(it instanceof ScoringDomain && it.subDomains != null){
                List<String> foundParts = getValidationKeyParts(it.subDomains, domainId);
                if(foundParts.size() > 0){
                    parts.add("[" + index + "]");
                    parts.addAll(foundParts);
                }
            }

            if(it.id == domainId){
                parts.add("[" + index + "]");
            }
        }

        return parts;
    }

    private static String getValidationKey(List<Domain> domains, String domainId){

        String validationKey = "";
        List<String> parts = getValidationKeyParts(domains, domainId);
        if(parts.size() == 0){
            return null;
        }

        parts.eachWithIndex{ String entry, int i ->
            String memberName = "domains";

            if(i > 0){
                memberName = "subDomains";
                validationKey += ".";
            }

            validationKey += memberName + entry;
        }

        return validationKey += ".rubric";
    }
}
