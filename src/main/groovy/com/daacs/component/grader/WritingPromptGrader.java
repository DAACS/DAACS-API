package com.daacs.component.grader;

import com.daacs.framework.exception.IncompatibleTypeException;
import com.daacs.framework.exception.InvalidObjectException;
import com.daacs.framework.exception.NALightsideOutputException;
import com.daacs.model.assessment.*;
import com.daacs.model.assessment.user.CompletionScore;
import com.daacs.model.assessment.user.CompletionStatus;
import com.daacs.model.assessment.user.DomainScore;
import com.daacs.model.assessment.user.WritingPromptUserAssessment;
import com.daacs.service.LightSideService;
import com.lambdista.util.Try;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chostetter on 7/27/16.
 */
public class WritingPromptGrader extends ManualGrader<WritingPromptUserAssessment, WritingAssessment> {

    private WritingPromptUserAssessment userAssessment;
    private WritingAssessment assessment;

    private LightSideService lightSideService;

    public WritingPromptGrader(
            WritingPromptUserAssessment userAssessment,
            WritingAssessment assessment,
            LightSideService lightSideService) {

        this.userAssessment = userAssessment;
        this.assessment = assessment;
        this.lightSideService = lightSideService;
    }

    @Override
    public Try<WritingPromptUserAssessment> grade(){

        Try<List<DomainScore>> maybeDomainScores = getDomainScores();
        if(maybeDomainScores.isFailure()){
            return new Try.Failure<>(maybeDomainScores.failed().get());
        }

        userAssessment.setDomainScores(maybeDomainScores.get());

        if (userAssessment.getOverallScore() == null || userAssessment.getScoringType() == ScoringType.LIGHTSIDE){
            Try<CompletionScore> maybeOverallScore = getAverageCompletionScore(maybeDomainScores.get());
            if (maybeOverallScore.isFailure()){
                return new Try.Failure<>(maybeOverallScore.failed().get());
            }

            userAssessment.setOverallScore(maybeOverallScore.get());
        }

        userAssessment.setStatus(CompletionStatus.GRADED);
        userAssessment.setGradingError(null);

        return new Try.Success<>(userAssessment);
    }

    protected Try<List<DomainScore>> getDomainScores(){

        List<DomainScore> domainScores = new ArrayList<>();

        for(Domain domain : assessment.getDomains()) {
            if(!(domain instanceof ScoringDomain)) continue;

            Try<DomainScore> maybeDomainScore = getDomainScore((ScoringDomain) domain);
            if(maybeDomainScore.isFailure()){
                return new Try.Failure<>(maybeDomainScore.failed().get());
            }

            domainScores.add(maybeDomainScore.get());
        }

        return new Try.Success<>(domainScores);
    }

    protected Try<DomainScore> getDomainScore(ScoringDomain domain){
        DomainScore domainScore = new DomainScore();
        domainScore.setDomainId(domain.getId());

        List<DomainScore> subDomainScores = new ArrayList<>();
        if(domain.getSubDomains() != null){
            for(Domain subDomain : domain.getSubDomains()){
                if(!(subDomain instanceof ScoringDomain)) continue;

                Try<DomainScore> maybeSubDomainScore = getDomainScore((ScoringDomain) subDomain);
                if(maybeSubDomainScore.isFailure()){
                    return maybeSubDomainScore;
                }

                subDomainScores.add(maybeSubDomainScore.get());
            }
        }

        domainScore.setSubDomainScores(subDomainScores);

        Try<CompletionScore> maybeCompletionScore;
        if(domain.getScoreIsSubDomainAverage()) {
            maybeCompletionScore = getAverageCompletionScore(subDomainScores);
            if(maybeCompletionScore.isFailure()){
                return new Try.Failure<>(maybeCompletionScore.failed().get());
            }

            domainScore.setRubricScore(maybeCompletionScore.get());

            return new Try.Success<>(domainScore);
        }

        switch(userAssessment.getScoringType()){
            case MANUAL:
                //assign existing domainScore
                domainScore = getExistingDomainScore(userAssessment.getDomainScores(), domain.getId());
                break;

            case LIGHTSIDE:
                if(!assessment.getLightSideConfig().getDomainModels().containsKey(domain.getId())){
                    return new Try.Failure<>(new InvalidObjectException("Assessment", "No LightSide model specified for domain " + domain.getId()));
                }

                String modelFileName = assessment.getLightSideConfig().getDomainModels().get(domain.getId());
                maybeCompletionScore = runLightSide(modelFileName);
                if(maybeCompletionScore.isFailure()){
                    return new Try.Failure<>(maybeCompletionScore.failed().get());
                }

                domainScore.setRubricScore(maybeCompletionScore.get());
                break;

            default:
                return new Try.Failure<>(new IncompatibleTypeException("WritingPromptUserAssessment", new ScoringType[]{ ScoringType.MANUAL, ScoringType.LIGHTSIDE }, assessment.getScoringType()));
        }

        return new Try.Success<>(domainScore);
    }

    protected Try<CompletionScore> runLightSide(String modelFileName){

        String inputFileName = buildFileName(modelFileName, false);

        Try<Void> maybeCreatedInputFile = lightSideService.createInputFile(inputFileName, userAssessment.getWritingPrompt().getSample());
        if(maybeCreatedInputFile.isFailure()){
            return new Try.Failure<>(maybeCreatedInputFile.failed().get());
        }

        String outputFileName = buildFileName(modelFileName, true);

        Try<Void> maybePredicted = lightSideService.predict(modelFileName, inputFileName, outputFileName);
        if(maybePredicted.isFailure()){
            return new Try.Failure<>(maybePredicted.failed().get());
        }

        Try<CompletionScore> maybeReadOutput = lightSideService.readOutputFile(outputFileName);
        if(maybeReadOutput.isFailure()){
            if(maybeReadOutput.failed().get() instanceof NALightsideOutputException){
                userAssessment.setScoringType(ScoringType.MANUAL);
            }
            return maybeReadOutput;
        }

        Try<Void> maybeCleanedUp = lightSideService.cleanUpFiles(inputFileName, outputFileName);
        if(maybeCleanedUp.isFailure()){
            return new Try.Failure<>(maybeCleanedUp.failed().get());
        }

        return maybeReadOutput;

    }

    protected String buildFileName(String modelFileName, boolean isOutputFile){
        return assessment.getId() + "_" + modelFileName.replaceAll("\\.", "_") + (isOutputFile? "_out" : "") + ".csv";
    }
}
