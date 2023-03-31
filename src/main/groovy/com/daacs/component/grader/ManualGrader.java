package com.daacs.component.grader;

import com.daacs.framework.exception.InvalidObjectException;
import com.daacs.model.assessment.Assessment;
import com.daacs.model.assessment.user.CompletionScore;
import com.daacs.model.assessment.user.DomainScore;
import com.daacs.model.assessment.user.UserAssessment;
import com.lambdista.util.Try;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by alandistasio on 10/11/16.
 */
public abstract class ManualGrader<U extends UserAssessment, T extends Assessment> implements Grader<U, T> {

    @Override
    public abstract Try<U> grade();

    @Override
    public Try<CompletionScore> getAverageCompletionScore(List<DomainScore> domainScores) {
        if (domainScores.size() == 0) {
            return new Try.Failure<>(new InvalidObjectException("domainScores", "domainScores must be provided"));
        }

        List<DomainScore> invalidDomainScores = domainScores.stream()
                .filter(domainScore -> domainScore.getRubricScore() == null)
                .collect(Collectors.toList());

        if(invalidDomainScores.size() > 0){
            return new Try.Failure<>(new InvalidObjectException("domainScores", "Invalid domainScores provided - all averaged parent domains must contain subdomains with rubricScores"));
        }

        //average them out!
        double averageDomainScore = domainScores
                .stream()
                .mapToInt(domainScore -> domainScore.getRubricScore().getRawVal())
                .average()
                .getAsDouble();

        int roundedScore = Math.toIntExact(Math.round(averageDomainScore));
        //int roundedScore = Math.toIntExact(Math.round(averageDomainScore - 0.01));
        //int roundedScore = Math.toIntExact(Math.floor(averageDomainScore));

        Optional<CompletionScore> completionScore = Arrays.stream(CompletionScore.values())
                .filter(it -> it.getRawVal() == roundedScore)
                .findFirst();

        if (!completionScore.isPresent()){
            return new Try.Failure<>(new InvalidObjectException("CompletionScore", MessageFormat.format("Unable to average domain scores; no CompletionScore with rawVal of {0}", roundedScore)));
        }

        return new Try.Success<>(completionScore.get());
    }

    protected DomainScore getExistingDomainScore(List<DomainScore> domainScores, String domainId){
        for(DomainScore domainScore : domainScores) {
            if(domainScore.getDomainId().equals(domainId)){
                return domainScore;
            }

            if(domainScore.getSubDomainScores() != null && domainScore.getSubDomainScores().size() > 0){
                DomainScore maybeFoundDomainScore = getExistingDomainScore(domainScore.getSubDomainScores(), domainId);
                if(maybeFoundDomainScore != null){
                    return maybeFoundDomainScore;
                }
            }
        }

        return null;
    }

}
