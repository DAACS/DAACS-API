package com.daacs.component.grader;

import com.daacs.framework.exception.IncompatibleTypeException;
import com.daacs.framework.exception.InvalidObjectException;
import com.daacs.model.assessment.*;
import com.daacs.model.assessment.user.*;
import com.daacs.model.item.Item;
import com.daacs.model.item.ItemGroup;
import com.google.common.collect.Range;
import com.lambdista.util.Try;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by chostetter on 7/27/16.
 */
public class ItemGroupGrader<U extends ItemGroupUserAssessment<? extends ItemGroup>,
        T extends ItemGroupAssessment<? extends ItemGroup>> extends  ManualGrader<U, T> {

    private U userAssessment;
    private T assessment;

    public ItemGroupGrader(U userAssessment, T assessment) {
        this.userAssessment = userAssessment;
        this.assessment = assessment;
    }

    public Try<U> grade(){

        Try<List<DomainScore>> maybeDomainScores = getDomainScores();
        if(maybeDomainScores.isFailure()){
            return new Try.Failure<>(maybeDomainScores.failed().get());
        }

        userAssessment.setDomainScores(maybeDomainScores.get());

        Try<CompletionScore> maybeOverallScore;
        if(assessment.getScoringType() == ScoringType.MANUAL){
            maybeOverallScore = userAssessment.getOverallScore() != null? new Try.Success<>(userAssessment.getOverallScore()) : getAverageCompletionScore(maybeDomainScores.get());
        }
        else{
            maybeOverallScore = getOverallScore();
        }

        if (maybeOverallScore.isFailure()){
            return new Try.Failure<>(maybeOverallScore.failed().get());
        }

        userAssessment.setOverallScore(maybeOverallScore.get());

        userAssessment.setStatus(CompletionStatus.GRADED);
        userAssessment.setGradingError(null);

        return new Try.Success<>(userAssessment);
    }

    protected List<String> getScoringDomainIds(ScoringDomain domain){
        List<String> domainIds = new ArrayList<>();
        if(domain.getSubDomains() != null){
            for(Domain subDomain: domain.getSubDomains()){
                if(!(subDomain instanceof ScoringDomain)) continue;

                domainIds.addAll(getScoringDomainIds((ScoringDomain) subDomain));
            }
        }

        domainIds.add(domain.getId());
        return domainIds;
    }

    protected Try<List<DomainScore>> getDomainScores(){
        List<DomainScore> domainScores = new ArrayList<>();

        Map<String, DomainScoreDetails> domainScoreDetails = getDomainScoreDetails(userAssessment.getItemGroups());
        for(Domain domain : assessment.getDomains()) {
            if(!(domain instanceof ScoringDomain)) continue;

            Try<DomainScore> maybeDomainScore = getDomainScore((ScoringDomain) domain, domainScoreDetails);
            if(maybeDomainScore.isFailure()){
                return new Try.Failure<>(maybeDomainScore.failed().get());
            }

            //only include domains that were scored
            if(maybeDomainScore.get().getRubricScore() != null){
                domainScores.add(maybeDomainScore.get());
            }
        }

        return new Try.Success<>(domainScores);
    }

    protected Try<DomainScore> getDomainScore(ScoringDomain domain, Map<String, DomainScoreDetails> domainScoreDetailsMap){
        List<DomainScore> subDomainScores = new ArrayList<>();
        if(domain.getSubDomains() != null){
            for(Domain subDomain : domain.getSubDomains()){
                if(!(subDomain instanceof ScoringDomain)) continue;

                Try<DomainScore> maybeSubDomainScore = getDomainScore((ScoringDomain) subDomain, domainScoreDetailsMap);
                if(maybeSubDomainScore.isFailure()){
                    return maybeSubDomainScore;
                }

                //only include domains that were scored
                if(maybeSubDomainScore.get().getRubricScore() != null){
                    subDomainScores.add(maybeSubDomainScore.get());
                }
            }
        }

        DomainScore domainScore = new DomainScore();
        domainScore.setDomainId(domain.getId());
        domainScore.setSubDomainScores(subDomainScores.size() > 0? subDomainScores : null);


        if(domain.getScoreIsSubDomainAverage()) {
            Try<CompletionScore> maybeCompletionScore = getAverageCompletionScore(subDomainScores);
            if(maybeCompletionScore.isFailure()){
                return new Try.Failure<>(maybeCompletionScore.failed().get());
            }

            domainScore.setRubricScore(maybeCompletionScore.get());
            return new Try.Success<>(domainScore);
        }

        if(assessment.getScoringType() == ScoringType.MANUAL){
            DomainScore existingDomainScore = getExistingDomainScore(userAssessment.getDomainScores(), domain.getId());
            domainScore.setRubricScore(existingDomainScore.getRubricScore());
            return new Try.Success<>(domainScore);
        }

        int sumScore = 0;
        int numQuestions = 0;
        List<String> scoringDomainIds = getScoringDomainIds(domain);

        for(Map.Entry<String, DomainScoreDetails> detailsEntry : domainScoreDetailsMap.entrySet()){
            String domainId = detailsEntry.getKey();
            if(!scoringDomainIds.contains(domainId)){
                continue;
            }

            sumScore += detailsEntry.getValue().getScoreSum();
            numQuestions += detailsEntry.getValue().getNumQuestions();
        }

        switch(assessment.getScoringType()){
            case SUM:
                if(numQuestions > 0){
                    domainScore.setRawScore((double) sumScore);
                }
                break;

            case AVERAGE:
                if(numQuestions > 0){
                    domainScore.setRawScore((double) sumScore / (double) numQuestions);
                }
                break;

            default:
                return new Try.Failure<>(new IncompatibleTypeException("Assessment", new ScoringType[]{ ScoringType.SUM, ScoringType.AVERAGE }, assessment.getScoringType()));
        }

        if(domainScore.getRawScore() != null){
            Optional<CompletionScore> rubricScore = getRubricScore(domainScore.getRawScore(), domain);
            if(!rubricScore.isPresent()){
                return new Try.Failure<>(new InvalidObjectException("Assessment", "Unable to determine rubric score for domainId " + domain.getId() + ", " + domainScore.getRawScore()));
            }

            domainScore.setRubricScore(rubricScore.get());
        }

        return new Try.Success<>(domainScore);
    }

    protected Try<CompletionScore> getOverallScore(){

        Rubric overallRubric = assessment.getOverallRubric();
        if(overallRubric.getCompletionScoreMap() == null){
            return new Try.Failure<>(new InvalidObjectException("Assessment", "Assessment has invalid overallRubric - no completionScoreMap"));
        }

        Map<String, DomainScoreDetails> domainScoreDetails = getDomainScoreDetails(userAssessment.getItemGroups());
        DomainScoreDetails overallScoreDetails = new DomainScoreDetails();

        domainScoreDetails.entrySet().forEach(entry -> {
            overallScoreDetails.incrementNumQuestions(entry.getValue().getNumQuestions());
            overallScoreDetails.addScore(entry.getValue().getScoreSum());
        });


        Map<CompletionScore, Range<Double>> completionScoreMap = overallRubric.getCompletionScoreMap();

        Double rawScore;
        switch(assessment.getScoringType()){
            case SUM:
                rawScore = (double) overallScoreDetails.getScoreSum();
                break;

            case AVERAGE:
                int numQuestions = overallScoreDetails.getNumQuestions();

                if(numQuestions <= 0){
                    return new Try.Failure<>(new InvalidObjectException("UserAssessment", "no questions answered"));
                }

                rawScore = (double) overallScoreDetails.getScoreSum() / numQuestions;

                break;

            default:
                return new Try.Failure<>(new IncompatibleTypeException("Assessment", new ScoringType[]{ ScoringType.SUM, ScoringType.AVERAGE }, assessment.getScoringType()));
        }

        Optional<Map.Entry<CompletionScore, Range<Double>>> completionScoreEntry = completionScoreMap.entrySet().stream()
                .filter(rangeEntry -> rangeEntry.getValue().contains(rawScore))
                .findFirst();

        if(!completionScoreEntry.isPresent()){
            return new Try.Failure<>(new InvalidObjectException("UserAssessment", "rubric does not contain range for score " + rawScore));
        }

        return new Try.Success<>(completionScoreEntry.get().getKey());
    }


    protected Optional<CompletionScore> getRubricScore(Double rawScore, ScoringDomain domain){

        if(rawScore == null){
            return Optional.empty();
        }

        Rubric rubric = domain.getRubric();

        Map<CompletionScore, Range<Double>> completionScoreMap = rubric.getCompletionScoreMap();
        Optional<Map.Entry<CompletionScore, Range<Double>>> completionScoreEntry = completionScoreMap.entrySet().stream()
                .filter(rangeEntry -> rangeEntry.getValue().contains(rawScore))
                .findFirst();

        if(!completionScoreEntry.isPresent()){
            return Optional.empty();
        }

        return Optional.of(completionScoreEntry.get().getKey());
    }


    protected Map<String, DomainScoreDetails> getDomainScoreDetails(List<? extends ItemGroup> itemGroups){
        Map<String, Domain> domainsById = assessment.getDomains().stream()
                .collect(Collectors.toMap(Domain::getId, it -> it));

        Map<String, DomainScoreDetails> domainScoreDetails = new HashMap<>();

        for(ItemGroup itemGroup : itemGroups){
            for(Item item : itemGroup.getItems()){
                Domain domain = domainsById.get(item.getDomainId());
                if(domain != null && domain.getDomainType() == DomainType.ANALYSIS){
                    continue;
                }

                if(!domainScoreDetails.containsKey(item.getDomainId())){
                    domainScoreDetails.put(item.getDomainId(), new DomainScoreDetails());
                }

                domainScoreDetails.get(item.getDomainId()).incrementNumQuestions();

                if(item.getChosenItemAnswer() != null){
                    domainScoreDetails.get(item.getDomainId()).addScore(item.getChosenItemAnswer().getScore());
                }

            }
        }

        return domainScoreDetails;
    }
}
