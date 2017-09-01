package com.daacs.service;

import com.daacs.component.GraderFactory;
import com.daacs.component.grader.Grader;
import com.daacs.framework.exception.IncompatibleTypeException;
import com.daacs.framework.exception.InvalidObjectException;
import com.daacs.model.assessment.*;
import com.daacs.model.assessment.user.*;
import com.daacs.repository.AssessmentRepository;
import com.daacs.repository.UserAssessmentRepository;
import com.lambdista.util.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by chostetter on 7/26/16.
 */
@Service
public class ScoringServiceImpl implements ScoringService {

    @Autowired
    private UserAssessmentRepository userAssessmentRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private GraderFactory graderFactory;

    private List<AssessmentType> endpointGradableAssessmentTypes = new ArrayList<AssessmentType>(){{
        add(AssessmentType.CAT);
        add(AssessmentType.LIKERT);
        add(AssessmentType.MULTIPLE_CHOICE);
    }};

    @Override
    public boolean canAutoGradeFromUpdate(UserAssessment userAssessment){
        return endpointGradableAssessmentTypes.contains(userAssessment.getAssessmentType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Try<UserAssessment> autoGradeUserAssessment(UserAssessment userAssessment){

        Try<Assessment> maybeAssessment = assessmentRepository.getAssessment(userAssessment.getAssessmentId());
        if (maybeAssessment.isFailure()){
            return new Try.Failure<>(maybeAssessment.failed().get());
        }

        Assessment assessment = maybeAssessment.get();
        Try<Grader> maybeGrader = getGrader(userAssessment, assessment);
        if (maybeGrader.isFailure()) {
            return new Try.Failure<>(maybeGrader.failed().get());
        }

        Try<UserAssessment> maybeGradedUserAssessment = maybeGrader.get().grade();

        if (maybeGradedUserAssessment.isFailure()){
            return new Try.Failure<>(maybeGradedUserAssessment.failed().get());
        }

        return maybeGradedUserAssessment;
    }

    private List<String> getSubmittedScoredDomainIds(List<DomainScore> domainScores){
        List<String> domainIds = new ArrayList<>();
        for(DomainScore domainScore: domainScores){
            if(domainScore.getSubDomainScores() != null){
                domainIds.addAll(getSubmittedScoredDomainIds(domainScore.getSubDomainScores()));
            }

            if(domainScore.getRubricScore() != null) {
                domainIds.add(domainScore.getDomainId());
            }
        }

        return domainIds;
    }

    private List<String> getScoredDomainIds(List<Domain> domains){
        List<String> domainIds = new ArrayList<>();
        for(Domain domain: domains){
            if(domain instanceof ScoringDomain){
                if(((ScoringDomain) domain).getSubDomains() != null) {
                    domainIds.addAll(getScoredDomainIds(((ScoringDomain) domain).getSubDomains()));
                }

                if(((ScoringDomain) domain).getScoreIsSubDomainAverage()) continue; //skip these
            }


            domainIds.add(domain.getId());
        }

        return domainIds;
    }

    @Override
    public Try<UserAssessment> manualGradeUserAssessment(String userId, String userAssessmentId, List<DomainScore> domainScores, CompletionScore overallScore){

        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getUserAssessmentById(userId, userAssessmentId);
        if (maybeUserAssessment.isFailure()){
            return new Try.Failure<>(maybeUserAssessment.failed().get());
        }

        UserAssessment userAssessment = maybeUserAssessment.get();

        Try<Assessment> maybeAssessment = assessmentRepository.getAssessment(userAssessment.getAssessmentId());
        if (maybeAssessment.isFailure()){
            return new Try.Failure<>(maybeAssessment.failed().get());
        }

        Assessment assessment = maybeAssessment.get();

        List<String> submittedScoredDomainIds = getSubmittedScoredDomainIds(domainScores);
        List<String> unencludedScoredDomainIds = getScoredDomainIds(assessment.getDomains()).stream()
                .filter(domainId -> !submittedScoredDomainIds.contains(domainId))
                .collect(Collectors.toList());

        if (domainScores.size() != assessment.getDomains().size() || unencludedScoredDomainIds.size() > 0){
            return new Try.Failure<>(new InvalidObjectException("UserAssessment", "Manually graded domain scores must contain scores for all domains on the assessment that aren't marked as 'ScoreIsSubDomainAverage = true'"));
        }

        userAssessment.setDomainScores(domainScores);
        userAssessment.setOverallScore(overallScore);
        Try<Grader> maybeGrader = getGrader(userAssessment, assessment);
        if (maybeGrader.isFailure()) {
            return new Try.Failure<>(maybeGrader.failed().get());
        }

        Grader grader = maybeGrader.get();
        Try<WritingPromptUserAssessment> maybeGradedUserAssessment = grader.grade();
        if (maybeGradedUserAssessment.isFailure()) {
            return new Try.Failure<>(maybeGradedUserAssessment.failed().get());
        }

        userAssessment = maybeGradedUserAssessment.get();

        userAssessment.setProgressPercentage(1.0);
        userAssessment.setCompletionDate(Instant.now());

        return new Try.Success<>(userAssessment);
    }

    private Try<Grader> getGrader(UserAssessment userAssessment, Assessment assessment) {
        switch(userAssessment.getAssessmentType()){

            case LIKERT:
            case MULTIPLE_CHOICE:
                return new Try.Success<>(graderFactory.getGrader(
                        (MultipleChoiceUserAssessment) userAssessment,
                        (MultipleChoiceAssessment) assessment));

            case CAT:
                return new Try.Success<>(graderFactory.getGrader(
                        (CATUserAssessment) userAssessment,
                        (CATAssessment) assessment));

            case WRITING_PROMPT:
                return new Try.Success<>(graderFactory.getGrader(
                        (WritingPromptUserAssessment) userAssessment,
                        (WritingAssessment) assessment));

            default:
                return new Try.Failure<>(new IncompatibleTypeException(
                        "UserAssessment",
                        new AssessmentType[]{ AssessmentType.LIKERT, AssessmentType.MULTIPLE_CHOICE, AssessmentType.CAT, AssessmentType.WRITING_PROMPT },
                        userAssessment.getAssessmentType()));

        }
    }
}
