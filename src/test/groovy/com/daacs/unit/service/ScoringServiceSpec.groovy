package com.daacs.unit.service

import com.daacs.component.GraderFactory
import com.daacs.component.grader.Grader
import com.daacs.framework.exception.InvalidObjectException
import com.daacs.model.assessment.*
import com.daacs.model.assessment.user.*
import com.daacs.repository.AssessmentRepository
import com.daacs.repository.UserAssessmentRepository
import com.daacs.service.InstructorClassService
import com.daacs.service.ScoringService
import com.daacs.service.ScoringServiceImpl
import com.lambdista.util.Try
import spock.lang.Specification
import spock.lang.Unroll
/**
 * Created by chostetter on 6/22/16.
 */
class ScoringServiceSpec extends Specification {

    UserAssessmentRepository userAssessmentRepository;
    InstructorClassService instructorClassService;
    AssessmentRepository assessmentRepository;
    GraderFactory graderFactory;

    ScoringService scoringService;

    String dummyUserId = UUID.randomUUID().toString()

    List<UserAssessment> dummyUserAssessments;
    List<Assessment> dummyAssessments;

    Grader grader;

    def setup(){
        dummyUserAssessments = [
                new CATUserAssessment(
                        assessmentId: "user-assessment-1",
                        assessmentType: AssessmentType.CAT
                ),
                new MultipleChoiceUserAssessment(
                        assessmentId: "user-assessment-2",
                        assessmentType: AssessmentType.MULTIPLE_CHOICE
                ),
                new WritingPromptUserAssessment(
                        assessmentId: "user-assessment-3",
                        assessmentType: AssessmentType.WRITING_PROMPT
                )
        ]

        dummyAssessments = [
                new CATAssessment(
                        id: "assessment-1",
                        assessmentType: AssessmentType.CAT,
                        domains: [
                                new ScoringDomain(id: "domain-1", label: "Domain #1")
                        ]
                ),
                new MultipleChoiceAssessment(
                        id: "assessment-2",
                        assessmentType: AssessmentType.MULTIPLE_CHOICE
                ),
                new WritingAssessment(
                        id: "assessment-3",
                        assessmentType: AssessmentType.WRITING_PROMPT
                )
        ]

        userAssessmentRepository = Mock(UserAssessmentRepository)
        assessmentRepository = Mock(AssessmentRepository)
        instructorClassService = Mock(InstructorClassService)
        graderFactory = Mock(GraderFactory)

        grader = Mock(Grader)
        graderFactory.getGrader(_, _) >> grader

        scoringService = new ScoringServiceImpl(userAssessmentRepository: userAssessmentRepository, assessmentRepository: assessmentRepository, instructorClassService:instructorClassService, graderFactory: graderFactory)

        userAssessmentRepository.getUserAssessmentById(dummyUserId, dummyUserAssessments.get(0).getId()) >> new Try.Success<UserAssessment>(dummyUserAssessments.get(0))
        assessmentRepository.getAssessment(dummyUserAssessments.get(0).getAssessmentId()) >> new Try.Success<Assessment>(dummyAssessments.get(0))
        grader.grade() >> {
            dummyUserAssessments.get(0).setStatus(CompletionStatus.GRADED)
            new Try.Success<UserAssessment>(dummyUserAssessments.get(0))
        }
    }

    @Unroll
    def "autoGradeUserAssessment: success"(int index){
        setup:
        UserAssessment userAssessment = dummyUserAssessments.get(index)
        Assessment assessment = dummyAssessments.get(index)

        when:
        Try<UserAssessment> maybeUserAssessment = scoringService.autoGradeUserAssessment(userAssessment)

        then:
        1 * assessmentRepository.getAssessment(userAssessment.getAssessmentId()) >> new Try.Success<Assessment>(assessment)
        1 * grader.grade() >> new Try.Success<UserAssessment>(userAssessment)
        1 * instructorClassService.classAssessmentTaken(_,_) >> new Try.Success<Void>(null)


        then:
        maybeUserAssessment.isSuccess()

        where:
        index << [0, 1, 2]
    }

    def "autoGradeUserAssessment: getAssessment fails, i fail"(){
        when:
        Try<UserAssessment> maybeUserAssessment = scoringService.autoGradeUserAssessment(dummyUserAssessments.get(0))

        then:
        1 * assessmentRepository.getAssessment(dummyUserAssessments.get(0).getAssessmentId()) >> new Try.Failure<Assessment>(new Exception())
        0 * grader.grade()

        then:
        maybeUserAssessment.isFailure()
    }

    def "autoGradeUserAssessment: grade fails, i fail"(){
        when:
        Try<UserAssessment> maybeUserAssessment = scoringService.autoGradeUserAssessment(dummyUserAssessments.get(0))

        then:
        1 * grader.grade() >> new Try.Failure<UserAssessment>(new Exception())

        then:
        maybeUserAssessment.isFailure()
    }

    def "manualGradeUserAssessment: success"(){
        setup:
        UserAssessment userAssessment = dummyUserAssessments.get(0)
        Assessment assessment = dummyAssessments.get(0)
        List<DomainScore> domainScores = [ new DomainScore(domainId: "domain-1", rubricScore: CompletionScore.MEDIUM) ]
        CompletionScore overallScore = CompletionScore.MEDIUM

        when:
        Try<UserAssessment> maybeUserAssessment = scoringService.manualGradeUserAssessment(dummyUserId, userAssessment.getId(), domainScores, overallScore)

        then:
        1 * userAssessmentRepository.getUserAssessmentById(dummyUserId, userAssessment.getId()) >> new Try.Success<UserAssessment>(userAssessment)
        1 * assessmentRepository.getAssessment(userAssessment.getAssessmentId()) >> new Try.Success<Assessment>(assessment)
        1 * instructorClassService.classAssessmentTaken(_,_) >> new Try.Success<Void>(null)

        then:
        maybeUserAssessment.isSuccess()
        maybeUserAssessment.get().domainScores == domainScores
        maybeUserAssessment.get().overallScore == overallScore
        maybeUserAssessment.get().status == CompletionStatus.GRADED
        maybeUserAssessment.get().completionDate != null
        maybeUserAssessment.get().progressPercentage == 1.0
    }

    def "manualGradeUserAssessment: fails on instructorClassService.classAssessmentTaken"(){
        setup:
        UserAssessment userAssessment = dummyUserAssessments.get(0)
        Assessment assessment = dummyAssessments.get(0)
        List<DomainScore> domainScores = [ new DomainScore(domainId: "domain-1", rubricScore: CompletionScore.MEDIUM) ]
        CompletionScore overallScore = CompletionScore.MEDIUM

        when:
        Try<UserAssessment> maybeUserAssessment = scoringService.manualGradeUserAssessment(dummyUserId, userAssessment.getId(), domainScores, overallScore)

        then:
        1 * userAssessmentRepository.getUserAssessmentById(dummyUserId, userAssessment.getId()) >> new Try.Success<UserAssessment>(userAssessment)
        1 * assessmentRepository.getAssessment(userAssessment.getAssessmentId()) >> new Try.Success<Assessment>(assessment)
        1 * instructorClassService.classAssessmentTaken(_,_) >> new Try.Failure<UserAssessment>(new Exception())

        then:
        maybeUserAssessment.isFailure()
    }

    def "manualGradeUserAssessment: fails on getUserAssessmentById, i fail"(){
        setup:
        UserAssessment userAssessment = dummyUserAssessments.get(0)
        List<DomainScore> domainScores = [ new DomainScore(domainId: "domain-1", rubricScore: CompletionScore.MEDIUM) ]
        CompletionScore overallScore = CompletionScore.MEDIUM

        when:
        Try<UserAssessment> maybeUserAssessment = scoringService.manualGradeUserAssessment(dummyUserId, userAssessment.getId(), domainScores, overallScore)

        then:
        1 * userAssessmentRepository.getUserAssessmentById(dummyUserId, userAssessment.getId()) >> new Try.Failure<UserAssessment>(new Exception())
        0 * assessmentRepository.getAssessment(_)
        0 * instructorClassService.classAssessmentTaken(_,_)

        then:
        maybeUserAssessment.isFailure()
    }

    def "manualGradeUserAssessment: fails on getAssessment, i fail"(){
        setup:
        UserAssessment userAssessment = dummyUserAssessments.get(0)
        List<DomainScore> domainScores = [ new DomainScore(domainId: "domain-1", rubricScore: CompletionScore.MEDIUM) ]
        CompletionScore overallScore = CompletionScore.MEDIUM

        when:
        Try<UserAssessment> maybeUserAssessment = scoringService.manualGradeUserAssessment(dummyUserId, userAssessment.getId(), domainScores, overallScore)

        then:
        1 * assessmentRepository.getAssessment(userAssessment.getAssessmentId()) >> new Try.Failure<Assessment>(new Exception())

        then:
        maybeUserAssessment.isFailure()
    }

    def "manualGradeUserAssessment: fails on if we haven't included all domains"(){
        setup:
        UserAssessment userAssessment = dummyUserAssessments.get(0)
        List<DomainScore> domainScores = [ new DomainScore(domainId: "domain-1", rubricScore: CompletionScore.MEDIUM), new DomainScore(domainId: "domain-2", rubricScore: CompletionScore.MEDIUM) ]
        CompletionScore overallScore = CompletionScore.MEDIUM

        when:
        Try<UserAssessment> maybeUserAssessment = scoringService.manualGradeUserAssessment(dummyUserId, userAssessment.getId(), domainScores, overallScore)

        then:
        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof InvalidObjectException
    }

    def "manualGradeUserAssessment: fails on if we haven't included all domains #2"(){
        setup:
        UserAssessment userAssessment = dummyUserAssessments.get(0)
        List<DomainScore> domainScores = [ ]
        CompletionScore overallScore = CompletionScore.MEDIUM

        when:
        Try<UserAssessment> maybeUserAssessment = scoringService.manualGradeUserAssessment(dummyUserId, userAssessment.getId(), domainScores, overallScore)

        then:
        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof InvalidObjectException
    }
}
