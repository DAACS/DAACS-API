package com.daacs.unit.component.grader

import com.daacs.component.grader.WritingPromptGrader
import com.daacs.framework.exception.InvalidObjectException
import com.daacs.model.assessment.*
import com.daacs.model.assessment.user.*
import com.daacs.model.item.WritingPrompt
import com.daacs.service.LightSideService
import com.lambdista.util.Try
import spock.lang.Specification
import spock.lang.Unroll
/**
 * Created by chostetter on 6/22/16.
 */
class WritingPromptGraderSpec extends Specification {

    WritingPromptGrader grader
    WritingPromptGrader spiedGrader

    WritingPromptUserAssessment userAssessment
    WritingAssessment assessment
    LightSideService lightSideService

    String modelFileName = "overall.model.xml";
    String inputFileName
    String outputFileName

    def setup(){
        lightSideService = Mock(LightSideService)

        userAssessment = new WritingPromptUserAssessment(writingPrompt: new WritingPrompt(sample: "sample"))
        assessment = new WritingAssessment(scoringType: ScoringType.MANUAL, domains: [ new ScoringDomain(id:"domain-1") ], lightSideConfig: new LightSideConfig(domainModels: ["domain-1": "domain-1.model.xml", "domain-2": "domain-2.model.xml"]))

        setupGrader()
    }

    def void setupGrader(){
        grader = new WritingPromptGrader(userAssessment, assessment, lightSideService)
        spiedGrader = Spy(WritingPromptGrader, constructorArgs: [userAssessment, assessment, lightSideService])
        inputFileName = grader.buildFileName(modelFileName, false);
        outputFileName = grader.buildFileName(modelFileName, true);
    }

    def "grader fails on unknown scoring types"(ScoringType scoringType, boolean expectFailure) {
        setup:
        setupGrader()
        assessment.scoringType = scoringType
        userAssessment.scoringType = scoringType
        spiedGrader.getAverageCompletionScore(_) >> new Try.Success<CompletionScore>(CompletionScore.HIGH)
        spiedGrader.runLightSide(_) >> new Try.Success<CompletionScore>(CompletionScore.MEDIUM)

        when:
        Try<UserAssessment> maybeUserAssessment = spiedGrader.grade()

        then:
        maybeUserAssessment.isFailure() == expectFailure

        where:
        scoringType           | expectFailure
        ScoringType.SUM       | true
        ScoringType.AVERAGE   | true
        ScoringType.LIGHTSIDE | false
        ScoringType.MANUAL    | false
    }

    def "runLightSide: success"(){
        when:
        Try<CompletionScore> maybeCompletionScore = spiedGrader.runLightSide(modelFileName);

        then:
        1 * lightSideService.createInputFile(inputFileName, userAssessment.getWritingPrompt().getSample()) >> new Try.Success<Void>(null)
        1 * lightSideService.predict(modelFileName, inputFileName, outputFileName) >> new Try.Success<Void>(null)
        1 * lightSideService.readOutputFile(outputFileName) >> new Try.Success<CompletionScore>(CompletionScore.HIGH)
        1 * lightSideService.cleanUpFiles(inputFileName, outputFileName) >> new Try.Success<Void>(null)

        then:
        maybeCompletionScore.isSuccess()
        maybeCompletionScore.get() == CompletionScore.HIGH
    }

    def "runLightSide: cleanUpFiles fails, i fail"(){
        when:
        Try<CompletionScore> maybeCompletionScore = spiedGrader.runLightSide(modelFileName);

        then:
        1 * lightSideService.createInputFile(inputFileName, userAssessment.getWritingPrompt().getSample()) >> new Try.Success<Void>(null)
        1 * lightSideService.predict(modelFileName, inputFileName, outputFileName) >> new Try.Success<Void>(null)
        1 * lightSideService.readOutputFile(outputFileName) >> new Try.Success<CompletionScore>(CompletionScore.HIGH)
        1 * lightSideService.cleanUpFiles(inputFileName, outputFileName) >> new Try.Failure<Void>(new Exception())

        then:
        maybeCompletionScore.isFailure()
    }

    def "runLightSide: readOutputFile fails, i fail"(){
        when:
        Try<CompletionScore> maybeCompletionScore = spiedGrader.runLightSide(modelFileName);

        then:
        1 * lightSideService.createInputFile(inputFileName, userAssessment.getWritingPrompt().getSample()) >> new Try.Success<Void>(null)
        1 * lightSideService.predict(modelFileName, inputFileName, outputFileName) >> new Try.Success<Void>(null)
        1 * lightSideService.readOutputFile(outputFileName) >> new Try.Failure<CompletionScore>(new Exception())
        0 * lightSideService.cleanUpFiles(*_)

        then:
        maybeCompletionScore.isFailure()
    }

    def "runLightSide: predict fails, i fail"(){
        when:
        Try<CompletionScore> maybeCompletionScore = spiedGrader.runLightSide(modelFileName);

        then:
        1 * lightSideService.createInputFile(inputFileName, userAssessment.getWritingPrompt().getSample()) >> new Try.Success<Void>(null)
        1 * lightSideService.predict(modelFileName, inputFileName, outputFileName) >> new Try.Failure<Void>(new Exception())
        0 * lightSideService.readOutputFile(*_)
        0 * lightSideService.cleanUpFiles(*_)

        then:
        maybeCompletionScore.isFailure()
    }

    def "runLightSide: createInputFile fails, i fail"(){
        when:
        Try<CompletionScore> maybeCompletionScore = spiedGrader.runLightSide(modelFileName);

        then:
        1 * lightSideService.createInputFile(inputFileName, userAssessment.getWritingPrompt().getSample()) >> new Try.Failure<Void>(new Exception())
        0 * lightSideService.predict(*_)
        0 * lightSideService.readOutputFile(*_)
        0 * lightSideService.cleanUpFiles(*_)

        then:
        maybeCompletionScore.isFailure()
    }

    def "getDomainScores: success"(){
        setup:
        assessment.setScoringType(ScoringType.LIGHTSIDE)
        userAssessment.setScoringType(ScoringType.LIGHTSIDE)

        when:
        Try<List<DomainScore>> maybeDomainScores = spiedGrader.getDomainScores()

        then:
        1 * spiedGrader.runLightSide(assessment.lightSideConfig.domainModels.get("domain-1")) >> new Try.Success<CompletionScore>(CompletionScore.HIGH)

        then:
        maybeDomainScores.isSuccess()
        maybeDomainScores.get().size() == 1
        maybeDomainScores.get().get(0).getDomainId() == "domain-1"
        maybeDomainScores.get().get(0).getRubricScore() == CompletionScore.HIGH
    }

    def "getDomainScores: runLightSide fails, i fail"(){
        setup:
        assessment.setScoringType(ScoringType.LIGHTSIDE)
        userAssessment.setScoringType(ScoringType.LIGHTSIDE)

        when:
        Try<List<DomainScore>> maybeDomainScores = spiedGrader.getDomainScores()

        then:
        1 * spiedGrader.runLightSide(assessment.lightSideConfig.domainModels.get("domain-1")) >> new Try.Failure<CompletionScore>(new Exception())

        then:
        maybeDomainScores.isFailure()
    }

    def "getDomainScores: no model file for domain, i fail"(){
        setup:
        assessment.setScoringType(ScoringType.LIGHTSIDE)
        userAssessment.setScoringType(ScoringType.LIGHTSIDE)
        assessment.lightSideConfig.domainModels.remove("domain-1")

        when:
        Try<List<DomainScore>> maybeDomainScores = spiedGrader.getDomainScores()

        then:
        maybeDomainScores.isFailure()
        maybeDomainScores.failed().get() instanceof InvalidObjectException
    }

    def "grade: lightside success"(){
        setup:
        setupGrader()
        assessment.scoringType = ScoringType.LIGHTSIDE
        userAssessment.scoringType = ScoringType.LIGHTSIDE

        when:
        Try<UserAssessment> maybeUserAssessment = spiedGrader.grade()

        then:
        1 * spiedGrader.getDomainScores() >> new Try.Success<List<DomainScore>>([ new DomainScore(domainId: "domain-1", rubricScore: CompletionScore.MEDIUM) ])
        1 * spiedGrader.getAverageCompletionScore(_) >> new Try.Success<CompletionScore>(CompletionScore.HIGH)

        then:
        maybeUserAssessment.isSuccess()
        maybeUserAssessment.get().getStatus() == CompletionStatus.GRADED
        maybeUserAssessment.get().getOverallScore() == CompletionScore.HIGH
        maybeUserAssessment.get().getDomainScores().size() == 1
        maybeUserAssessment.get().getDomainScores().get(0).getDomainId() == "domain-1"
        maybeUserAssessment.get().getDomainScores().get(0).getRubricScore() == CompletionScore.MEDIUM
    }

    def "grade: lightside getDomainScores fails, i fail"(){
        setup:
        setupGrader()
        assessment.scoringType = ScoringType.LIGHTSIDE
        userAssessment.scoringType = ScoringType.LIGHTSIDE

        when:
        Try<UserAssessment> maybeUserAssessment = spiedGrader.grade()

        then:
        1 * spiedGrader.getDomainScores() >> new Try.Failure<List<DomainScore>>(new Exception())
        0 * spiedGrader.getOverallScore(_)

        then:
        maybeUserAssessment.isFailure()
    }

    def "grade: lightside getOverallScore fails, i fail"(){
        setup:
        setupGrader()
        assessment.scoringType = ScoringType.LIGHTSIDE
        userAssessment.scoringType = ScoringType.LIGHTSIDE

        when:
        Try<UserAssessment> maybeUserAssessment = spiedGrader.grade()

        then:
        1 * spiedGrader.getDomainScores() >> new Try.Success<List<DomainScore>>([ new DomainScore(domainId: "domain-1", rubricScore: CompletionScore.MEDIUM) ])
        1 * spiedGrader.getAverageCompletionScore(_) >> new Try.Failure<CompletionScore>(new Exception())

        then:
        maybeUserAssessment.isFailure()
    }

    def "manualGradeUserAssessment: success with averaging for overallScore"(){
        setup:
        List<DomainScore> domainScores = [
                new DomainScore(domainId: "domain-1", rubricScore: CompletionScore.MEDIUM),
                new DomainScore(domainId: "domain-2", rubricScore: CompletionScore.HIGH),
                new DomainScore(domainId: "domain-3", rubricScore: CompletionScore.MEDIUM)
        ]

        when:
        Try<CompletionScore> maybeScore = grader.getAverageCompletionScore(domainScores)

        then:
        maybeScore.isSuccess()
        maybeScore.get() == CompletionScore.MEDIUM
    }

    def "manualGradeUserAssessment: fails if overallScore is null and no domains"(){
        setup:
        List<DomainScore> domainScores = []

        when:
        Try<CompletionScore> maybeScore = grader.getAverageCompletionScore(domainScores)

        then:
        maybeScore.isFailure()
    }

    @Unroll
    def "getDomainScore: scoreIsSubDomainAverage true, success"(CompletionScore domain1Score, CompletionScore domain2Score, CompletionScore expectedScore){
        setup:
        setupGrader()
        assessment.setScoringType(ScoringType.LIGHTSIDE)
        userAssessment.setScoringType(ScoringType.LIGHTSIDE)
        Domain domain = new ScoringDomain(id:"domain-3", scoreIsSubDomainAverage: true, subDomains: [new ScoringDomain(id:"domain-1"), new ScoringDomain(id:"domain-2")])

        when:
        Try<DomainScore> maybeDomainScore = spiedGrader.getDomainScore(domain)

        then:
        1 * spiedGrader.runLightSide(assessment.lightSideConfig.domainModels.get("domain-1")) >> new Try.Success<CompletionScore>(domain1Score)
        1 * spiedGrader.runLightSide(assessment.lightSideConfig.domainModels.get("domain-2")) >> new Try.Success<CompletionScore>(domain2Score)

        then:
        maybeDomainScore.isSuccess()
        maybeDomainScore.get().rubricScore == expectedScore

        where:
        domain1Score           | domain2Score             | expectedScore
        CompletionScore.HIGH   | CompletionScore.HIGH     | CompletionScore.HIGH
        CompletionScore.MEDIUM | CompletionScore.MEDIUM   | CompletionScore.MEDIUM
        CompletionScore.LOW    | CompletionScore.LOW      | CompletionScore.LOW
        CompletionScore.HIGH   | CompletionScore.MEDIUM   | CompletionScore.HIGH
        CompletionScore.LOW    | CompletionScore.MEDIUM   | CompletionScore.MEDIUM
        CompletionScore.LOW    | CompletionScore.HIGH     | CompletionScore.MEDIUM

    }
}
