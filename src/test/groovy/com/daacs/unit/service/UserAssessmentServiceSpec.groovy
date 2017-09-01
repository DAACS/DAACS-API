package com.daacs.service

import com.daacs.component.PrereqEvaluatorFactory
import com.daacs.component.prereq.AssessmentPrereqEvaluator
import com.daacs.framework.exception.*
import com.daacs.framework.serializer.DaacsOrikaMapper
import com.daacs.model.User
import com.daacs.model.assessment.*
import com.daacs.model.assessment.user.*
import com.daacs.model.dto.SaveWritingSampleRequest
import com.daacs.model.dto.UpdateUserAssessmentRequest
import com.daacs.model.item.*
import com.daacs.model.prereqs.AssessmentPrereq
import com.daacs.model.prereqs.PrereqType
import com.daacs.repository.AssessmentRepository
import com.daacs.repository.UserAssessmentRepository
import com.lambdista.util.Try
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant
import java.util.stream.Collectors

/**
 * Created by chostetter on 6/22/16.
 */
class UserAssessmentServiceSpec extends Specification {

    UserAssessmentRepository userAssessmentRepository
    AssessmentRepository assessmentRepository
    DaacsOrikaMapper daacsOrikaMapper
    ScoringService scoringService
    MessageService messageService
    CanvasService canvasService
    PrereqEvaluatorFactory prereqEvaluatorFactory

    UserAssessmentService userAssessmentService


    AssessmentPrereqEvaluator assessmentPrereqEvaluator

    User dummyUser = new User("username", "Mr", "Dummy");
    String dummyAssessmentId = UUID.randomUUID().toString();
    String dummyUserAssessmentId = UUID.randomUUID().toString();
    Instant dummyTakenDate = Instant.now();

    @Shared
    List<Assessment> dummyAssessments

    @Shared
    List<UserAssessment> dummyUserAssessments

    def setup(){
        dummyUserAssessments = [
                new CATUserAssessment(
                        assessmentId: "32c91abf-fc9b-4b41-ac4e-3f36b3e323d8",
                        assessmentType: AssessmentType.CAT,
                        completionDate: Instant.now(),
                        overallScore: CompletionScore.MEDIUM,
                        progressPercentage: 1.0,
                        status: CompletionStatus.COMPLETED,
                        takenDate: dummyTakenDate,
                        domainScores: [ new DomainScore(domainId: "domain", rubricScore: CompletionScore.MEDIUM) ],
                        itemGroups: [
                                new CATItemGroup(difficulty: Difficulty.EASY, items: [
                                        new Item(
                                                question: "abc?",
                                                domainId: "domain",
                                                possibleItemAnswers: [ new ItemAnswer(content: "abc", score: 1.0) ],
                                                chosenItemAnswerId: null
                                        )
                                ]),
                                new CATItemGroup(difficulty: Difficulty.MEDIUM, items: [
                                        new Item(
                                                question: "def?",
                                                domainId: "domain",
                                                possibleItemAnswers: [ new ItemAnswer(content: "def", score: 1.0) ],
                                                chosenItemAnswerId: null
                                        )
                                ])]
                ),
                new MultipleChoiceUserAssessment(
                        assessmentId: "32c91abf-fc9b-4b41-ac4e-3f36b3e323d8",
                        assessmentType: AssessmentType.MULTIPLE_CHOICE,
                        completionDate: Instant.now(),
                        overallScore: CompletionScore.MEDIUM,
                        progressPercentage: 1.0,
                        status: CompletionStatus.COMPLETED,
                        takenDate: dummyTakenDate,
                        domainScores: [ new DomainScore(domainId: "domain", rubricScore: CompletionScore.MEDIUM) ],
                        itemGroups: [
                                new ItemGroup(items: [
                                        new Item(
                                                question: "abc?",
                                                domainId: "domain",
                                                possibleItemAnswers: [ new ItemAnswer(content: "abc", score: 1.0) ],
                                                chosenItemAnswerId: null
                                        )
                                ]),
                                new ItemGroup(items: [
                                        new Item(
                                                question: "def?",
                                                domainId: "domain",
                                                possibleItemAnswers: [ new ItemAnswer(content: "def", score: 1.0) ],
                                                chosenItemAnswerId: null
                                        )
                                ])]
                ),
                new WritingPromptUserAssessment(
                        assessmentId: "32c91abf-fc9b-4b41-ac4e-3f36b3e323d0",
                        assessmentType: AssessmentType.WRITING_PROMPT,
                        completionDate: Instant.now(),
                        overallScore: CompletionScore.MEDIUM,
                        progressPercentage: 1.0,
                        status: CompletionStatus.IN_PROGRESS,
                        takenDate: dummyTakenDate,
                        domainScores: [ new DomainScore(domainId: "domain", rubricScore: CompletionScore.MEDIUM) ],
                        writingPrompt: new WritingPrompt(sample: "this is my writing sample")
                ),
                new WritingPromptUserAssessment(
                        assessmentId: "32c91abf-fc9b-4b41-ac4e-3f36b3e323d0",
                        assessmentType: AssessmentType.WRITING_PROMPT,
                        completionDate: Instant.now(),
                        overallScore: CompletionScore.MEDIUM,
                        progressPercentage: 1.0,
                        status: CompletionStatus.IN_PROGRESS,
                        takenDate: dummyTakenDate,
                        domainScores: [ new DomainScore(domainId: "domain", rubricScore: CompletionScore.MEDIUM) ]
                ),
                new WritingPromptUserAssessment(
                        assessmentId: "32c91abf-fc9b-4b41-ac4e-3f36b3e323d0",
                        assessmentType: AssessmentType.WRITING_PROMPT,
                        completionDate: Instant.now(),
                        overallScore: CompletionScore.MEDIUM,
                        progressPercentage: 1.0,
                        status: CompletionStatus.COMPLETED,
                        takenDate: dummyTakenDate,
                        domainScores: [ new DomainScore(domainId: "domain", rubricScore: CompletionScore.MEDIUM) ]
                )
        ]

        dummyAssessments = [
                new CATAssessment(
                        id: "32c91abf-fc9b-4b41-ac4e-3f36b3e323d8",
                        assessmentType: AssessmentType.CAT,
                        label: "Mathematics",
                        enabled: true,
                        prerequisites: [
                                new AssessmentPrereq(
                                        prereqType: PrereqType.ASSESSMENT,
                                        reason: "You must complete the writing assessment first!",
                                        assessmentCategory: AssessmentCategory.WRITING,
                                        statuses: [CompletionStatus.COMPLETED])
                        ]
                ),
                new MultipleChoiceAssessment(
                        id: "32c91abf-fc9b-4b41-ac4e-3f36b3e323d9",
                        assessmentType: AssessmentType.LIKERT,
                        label: "College Skills",
                        enabled: true
                ),
                new MultipleChoiceAssessment(
                        id: "32c91abf-fc9b-4b41-ac4e-3f36b3e323d1",
                        assessmentType: AssessmentType.MULTIPLE_CHOICE,
                        label: "Some other category",
                        enabled: true
                ),
                new WritingAssessment(
                        scoringType: ScoringType.MANUAL,
                        id: "32c91abf-fc9b-4b41-ac4e-3f36b3e323d0",
                        assessmentType: AssessmentType.WRITING_PROMPT,
                        label: "Writing",
                        enabled: true,
                        writingPrompt: new WritingPrompt(content: "content", minWords: 250)
                )
        ]

        messageService = Mock(MessageService)
        daacsOrikaMapper = new DaacsOrikaMapper();
        assessmentRepository = Mock(AssessmentRepository);
        userAssessmentRepository = Mock(UserAssessmentRepository);
        scoringService = Mock(ScoringService)
        prereqEvaluatorFactory = Mock(PrereqEvaluatorFactory)
        assessmentPrereqEvaluator = Mock(AssessmentPrereqEvaluator)
        prereqEvaluatorFactory.getAssessmentPrereqEvaluator(_) >> assessmentPrereqEvaluator
        assessmentPrereqEvaluator.getFailedPrereqs(_) >> []
        canvasService = Mock(CanvasService)
        canvasService.isEnabled() >> true

        userAssessmentService = new UserAssessmentServiceImpl(
                daacsOrikaMapper: daacsOrikaMapper,
                scoringService: scoringService,
                assessmentRepository: assessmentRepository,
                userAssessmentRepository: userAssessmentRepository,
                prereqEvaluatorFactory: prereqEvaluatorFactory,
                messageService: messageService,
                canvasService: canvasService)
    }

    def "getSummaries: returns mapped assessment summaries"(){
        when:
        Try<List<UserAssessmentSummary>> maybeUserAssessmentSummaries = userAssessmentService.getSummaries(dummyUser.id, dummyAssessmentId, dummyTakenDate)

        then:
        1 * userAssessmentRepository.getUserAssessments(dummyUser.id, dummyAssessmentId, dummyTakenDate) >> new Try.Success<List<UserAssessment>>(dummyUserAssessments)

        then:
        maybeUserAssessmentSummaries.isSuccess()
        maybeUserAssessmentSummaries.get().get(0).assessmentId == dummyUserAssessments.get(0).assessmentId
        maybeUserAssessmentSummaries.get().get(0).completionDate == dummyUserAssessments.get(0).completionDate
        maybeUserAssessmentSummaries.get().get(0).overallScore == dummyUserAssessments.get(0).overallScore
        maybeUserAssessmentSummaries.get().get(0).domainScores.get(0).rubricScore == dummyUserAssessments.get(0).domainScores.get(0).rubricScore
        maybeUserAssessmentSummaries.get().get(0).domainScores.get(0).domainId == dummyUserAssessments.get(0).domainScores.get(0).domainId
        maybeUserAssessmentSummaries.get().get(0).progressPercentage == dummyUserAssessments.get(0).progressPercentage
        maybeUserAssessmentSummaries.get().get(0).status == dummyUserAssessments.get(0).status
        maybeUserAssessmentSummaries.get().get(0).takenDate == dummyUserAssessments.get(0).takenDate
    }

    def "getSummaries: repo call fails, i fail"(){
        when:
        Try<List<UserAssessmentSummary>> maybeUserAssessmentSummaries = userAssessmentService.getSummaries(dummyUser.id, dummyAssessmentId, dummyTakenDate)

        then:
        1 * userAssessmentRepository.getUserAssessments(*_) >> new Try.Failure<List<UserAssessment>>(new Exception())

        then:
        maybeUserAssessmentSummaries.isFailure()
    }

    def "getSummaries 2: returns mapped assessment summaries"(){
        when:
        Try<List<UserAssessmentSummary>> maybeUserAssessmentSummaries = userAssessmentService.getSummaries(dummyUser.id, AssessmentCategory.MATHEMATICS, dummyTakenDate)

        then:
        1 * userAssessmentRepository.getUserAssessments(dummyUser.id, AssessmentCategory.MATHEMATICS, dummyTakenDate) >> new Try.Success<List<UserAssessment>>(dummyUserAssessments)

        then:
        maybeUserAssessmentSummaries.isSuccess()
        maybeUserAssessmentSummaries.get().get(0).assessmentId == dummyUserAssessments.get(0).assessmentId
        maybeUserAssessmentSummaries.get().get(0).completionDate == dummyUserAssessments.get(0).completionDate
        maybeUserAssessmentSummaries.get().get(0).overallScore == dummyUserAssessments.get(0).overallScore
        maybeUserAssessmentSummaries.get().get(0).domainScores.get(0).rubricScore == dummyUserAssessments.get(0).domainScores.get(0).rubricScore
        maybeUserAssessmentSummaries.get().get(0).domainScores.get(0).domainId == dummyUserAssessments.get(0).domainScores.get(0).domainId
        maybeUserAssessmentSummaries.get().get(0).progressPercentage == dummyUserAssessments.get(0).progressPercentage
        maybeUserAssessmentSummaries.get().get(0).status == dummyUserAssessments.get(0).status
        maybeUserAssessmentSummaries.get().get(0).takenDate == dummyUserAssessments.get(0).takenDate
    }

    def "getSummaries 2: repo call fails, i fail"(){
        when:
        Try<List<UserAssessmentSummary>> maybeUserAssessmentSummaries = userAssessmentService.getSummaries(dummyUser.id, AssessmentCategory.MATHEMATICS, dummyTakenDate)

        then:
        1 * userAssessmentRepository.getUserAssessments(*_) >> new Try.Failure<List<UserAssessment>>(new Exception())

        then:
        maybeUserAssessmentSummaries.isFailure()
    }

    def "createUserAssessment: creates a user assessment of correct type"(Assessment dummyAssessment, Class mappedClass){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentService.createUserAssessment(dummyUser, dummyAssessment.getId())

        then:
        1 * assessmentRepository.getAssessments(true, _) >> new Try.Success<List<Assessment>>(dummyAssessments)
        1 * userAssessmentRepository.getLatestUserAssessments(dummyUser.getId(), dummyAssessments.collect{ it.getId() }) >> new Try.Success<List<UserAssessment>>([])
        1 * userAssessmentRepository.getUserAssessments(dummyUser.getId()) >> new Try.Success<List<UserAssessment>>([])
        1 * userAssessmentRepository.insertUserAssessment(_) >> new Try.Success<Void>(null)

        then:
        maybeUserAssessment.isSuccess()
        mappedClass.isInstance(maybeUserAssessment.get())

        where:
        dummyAssessment         | mappedClass
        dummyAssessments.get(0) | CATUserAssessment
        dummyAssessments.get(1) | MultipleChoiceUserAssessment
        dummyAssessments.get(2) | MultipleChoiceUserAssessment
        dummyAssessments.get(3) | WritingPromptUserAssessment
    }

    def "createUserAssessment: if we have a failed prereq, fail"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentService.createUserAssessment(dummyUser, dummyAssessments.get(0).getId())

        then:
        1 * assessmentRepository.getAssessments(true, _) >> new Try.Success<List<Assessment>>(dummyAssessments)
        1 * userAssessmentRepository.getLatestUserAssessments(dummyUser.getId(), dummyAssessments.collect{ it.getId() }) >> new Try.Success<List<UserAssessment>>([])
        1 * userAssessmentRepository.getUserAssessments(dummyUser.getId()) >> new Try.Success<List<UserAssessment>>([])
        1 * assessmentPrereqEvaluator.getFailedPrereqs(_) >> [new AssessmentPrereq()]
        0 * userAssessmentRepository.insertUserAssessment(_)

        then:
        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof FailedPrereqException
    }

    @Unroll
    def "createUserAssessment: if latest user assessment is in progress or ungraded, return that instead"(boolean returnsExisting, UserAssessment latestUserAssessment){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentService.createUserAssessment(dummyUser, dummyAssessments.get(0).getId())

        then:
        1 * assessmentRepository.getAssessments(true, _) >> new Try.Success<List<Assessment>>(dummyAssessments)
        1 * userAssessmentRepository.getLatestUserAssessments(dummyUser.getId(), dummyAssessments.collect{ it.getId() }) >> new Try.Success<List<UserAssessment>>([latestUserAssessment])

        if(!returnsExisting){
            1 * userAssessmentRepository.getUserAssessments(dummyUser.getId()) >> new Try.Success<List<UserAssessment>>([latestUserAssessment])
            1 * userAssessmentRepository.insertUserAssessment(_) >> new Try.Success<Void>(null)
        }
        else{
            0 * userAssessmentRepository.getUserAssessments(_)
            0 * userAssessmentRepository.insertUserAssessment(_)
        }

        then:
        maybeUserAssessment.isSuccess()

        if(returnsExisting){
            maybeUserAssessment.get() == latestUserAssessment
        }
        else{
            maybeUserAssessment.get() != latestUserAssessment
        }

        where:
        returnsExisting | latestUserAssessment
        true            | new CATUserAssessment(status: CompletionStatus.IN_PROGRESS, assessmentId: dummyAssessments.get(0).getId())
        true            | new CATUserAssessment(status: CompletionStatus.COMPLETED, assessmentId: dummyAssessments.get(0).getId())
        false           | new CATUserAssessment(status: CompletionStatus.GRADED, assessmentId: dummyAssessments.get(0).getId())

    }

    def "createUserAssessment: getAssessment fails, i fail"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentService.createUserAssessment(dummyUser, dummyAssessments.get(0).getId())

        then:
        1 * assessmentRepository.getAssessments(true, _) >> new Try.Failure<List<Assessment>>(new Exception())
        0 * userAssessmentRepository.getUserAssessments(*_)
        0 * userAssessmentRepository.getLatestUserAssessment(*_)
        0 * userAssessmentRepository.insertUserAssessment(_)

        then:
        maybeUserAssessment.isFailure()
    }

    def "createUserAssessment: getAssessments returns none, produces not found error"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentService.createUserAssessment(dummyUser, dummyAssessments.get(0).getId())

        then:
        1 * assessmentRepository.getAssessments(true, _) >> new Try.Success<List<Assessment>>([])
        0 * userAssessmentRepository.getLatestUserAssessment(*_)
        0 * userAssessmentRepository.insertUserAssessment(_)

        then:
        maybeUserAssessment.isFailure()
    }

    def "createUserAssessment: getUserAssessments fails on something other than RepoNotFound, i fail"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentService.createUserAssessment(dummyUser, dummyAssessments.get(0).getId())

        then:
        1 * assessmentRepository.getAssessments(true, _) >> new Try.Success<List<Assessment>>(dummyAssessments)
        1 * userAssessmentRepository.getLatestUserAssessments(dummyUser.getId(), dummyAssessments.collect{ it.getId() }) >> new Try.Failure<List<UserAssessment>>(new Exception())
        0 * userAssessmentRepository.insertUserAssessment(_)

        then:
        maybeUserAssessment.isFailure()
    }

    def "createUserAssessment: insertUserAssessment fails, i fail"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentService.createUserAssessment(dummyUser, dummyAssessments.get(0).getId())

        then:
        1 * assessmentRepository.getAssessments(true, _) >> new Try.Success<List<Assessment>>(dummyAssessments)
        1 * userAssessmentRepository.getLatestUserAssessments(dummyUser.getId(), dummyAssessments.collect{ it.getId() }) >> new Try.Success<List<UserAssessment>>([])
        1 * userAssessmentRepository.getUserAssessments(dummyUser.getId()) >> new Try.Success<List<UserAssessment>>([])
        1 * userAssessmentRepository.insertUserAssessment(_) >> new Try.Failure<Void>(null)

        then:
        maybeUserAssessment.isFailure()
    }

    def "getAnswers: returns answers for CATUserAssessment"(){
        setup:
        CATUserAssessment userAssessment = dummyUserAssessments.get(0);

        when:
        Try<List<ItemGroup>> maybeUserAssessmentAnswers = userAssessmentService.getAnswers(dummyUser.getId(), userAssessment.getId(), "domain", dummyTakenDate)

        then:
        1 * userAssessmentRepository.getUserAssessment(dummyUser.getId(), userAssessment.getId(), dummyTakenDate) >> new Try.Success<UserAssessment>(userAssessment)

        then:
        maybeUserAssessmentAnswers.isSuccess()
        List<ItemGroup> returnedItemGroups = maybeUserAssessmentAnswers.get()
        returnedItemGroups.size() > 0
        returnedItemGroups.get(0).id == userAssessment.itemGroups.get(0).id
    }

    def "getAnswers: fails if IN_PROGRESS"(){
        setup:
        CATUserAssessment userAssessment = dummyUserAssessments.get(0);
        userAssessment.status = CompletionStatus.IN_PROGRESS

        when:
        Try<List<ItemGroup>> maybeUserAssessmentAnswers = userAssessmentService.getAnswers(dummyUser.getId(), userAssessment.getId(), "domain", dummyTakenDate)

        then:
        1 * userAssessmentRepository.getUserAssessment(dummyUser.getId(), userAssessment.getId(), dummyTakenDate) >> new Try.Success<UserAssessment>(userAssessment)

        then:
        maybeUserAssessmentAnswers.isFailure()
        maybeUserAssessmentAnswers.failed().get() instanceof IncompatibleStatusException
    }

    def "getAnswers: returns answers for MultipleChoiceUserAssessment"(){
        setup:
        MultipleChoiceUserAssessment userAssessment = dummyUserAssessments.get(1);

        when:
        Try<List<ItemGroup>> maybeUserAssessmentAnswers = userAssessmentService.getAnswers(dummyUser.getId(), userAssessment.getId(), "domain", dummyTakenDate)

        then:
        1 * userAssessmentRepository.getUserAssessment(dummyUser.getId(), userAssessment.getId(), dummyTakenDate) >> new Try.Success<UserAssessment>(userAssessment)

        then:
        maybeUserAssessmentAnswers.isSuccess()
        List<ItemGroup> returnedItemGroups = maybeUserAssessmentAnswers.get()
        returnedItemGroups.size() > 0
        returnedItemGroups.get(0).id == userAssessment.itemGroups.get(0).id
    }

    def "getAnswers: fails for WritingPromptUserAssessment"(){
        setup:
        WritingPromptUserAssessment userAssessment = dummyUserAssessments.get(2);

        when:
        Try<List<ItemGroup>> maybeUserAssessmentAnswers = userAssessmentService.getAnswers(dummyUser.getId(), userAssessment.getId(), "domain", dummyTakenDate)

        then:
        1 * userAssessmentRepository.getUserAssessment(dummyUser.getId(), userAssessment.getId(), dummyTakenDate) >> new Try.Success<UserAssessment>(userAssessment)

        then:
        maybeUserAssessmentAnswers.isFailure()
    }

    def "getAnswers: getUserAssessment fails, i fail"(){
        setup:
        MultipleChoiceUserAssessment userAssessment = dummyUserAssessments.get(1);

        when:
        Try<List<ItemGroup>> maybeUserAssessmentAnswers = userAssessmentService.getAnswers(dummyUser.getId(), userAssessment.getId(), "domain", dummyTakenDate)

        then:
        1 * userAssessmentRepository.getUserAssessment(*_) >> new Try.Failure<UserAssessment>(new Exception())

        then:
        maybeUserAssessmentAnswers.isFailure()
    }

    def "getUserAssessmentWritingSample: returns writing sample for WritingPromptUserAssessment"(){
        setup:
        WritingPromptUserAssessment userAssessment = dummyUserAssessments.get(2);

        when:
        Try<WritingPrompt> maybeWritingSample = userAssessmentService.getWritingSample(dummyUser.getId(), userAssessment.getId(), dummyTakenDate)

        then:
        1 * userAssessmentRepository.getUserAssessment(dummyUser.getId(), userAssessment.getId(), dummyTakenDate) >> new Try.Success<UserAssessment>(userAssessment)

        then:
        maybeWritingSample.isSuccess()
        maybeWritingSample.get().sample == userAssessment.writingPrompt.sample
    }

    def "getUserAssessmentWritingSample: returns null if writingSample is null"(){
        setup:
        WritingPromptUserAssessment userAssessment = dummyUserAssessments.get(2);
        userAssessment.writingPrompt = null

        when:
        Try<String> maybeWritingSample = userAssessmentService.getWritingSample(dummyUser.getId(), userAssessment.getId(), dummyTakenDate)

        then:
        1 * userAssessmentRepository.getUserAssessment(dummyUser.getId(), userAssessment.getId(), dummyTakenDate) >> new Try.Success<UserAssessment>(userAssessment)

        then:
        maybeWritingSample.isSuccess()
        maybeWritingSample.get() == null
    }

    def "getUserAssessmentWritingSample: fails for anything but WritingPromptUserAssessment"(UserAssessment userAssessment){
        when:
        Try<String> maybeWritingSample = userAssessmentService.getWritingSample(dummyUser.getId(), userAssessment.getId(), dummyTakenDate)

        then:
        1 * userAssessmentRepository.getUserAssessment(dummyUser.getId(), userAssessment.getId(), dummyTakenDate) >> new Try.Success<UserAssessment>(userAssessment)

        then:
        maybeWritingSample.isSuccess() == (userAssessment instanceof WritingPromptUserAssessment)

        where:
        userAssessment << dummyUserAssessments
    }

    def "getUserAssessmentWritingSample: getUserAssessment fails, i fail"(){
        when:
        Try<String> maybeWritingSample = userAssessmentService.getWritingSample(dummyUser.getId(), dummyUserAssessments.get(2).getId(), dummyTakenDate)

        then:
        1 * userAssessmentRepository.getUserAssessment(_, _, _) >> new Try.Failure<UserAssessment>(new Exception())

        then:
        maybeWritingSample.isFailure()
    }

    def "getUserAssessmentTakenDates: returns all dates taken for an assessment"(){
        setup:
        List<Instant> takenDates = [ Instant.parse("2015-01-02T00:00:00.000Z"), Instant.parse("2015-01-03T00:00:00.000Z"), Instant.parse("2015-01-04T00:00:00.000Z") ]

        when:
        Try<List<UserAssessmentTakenDate>> maybeTakenDates = userAssessmentService.getTakenDates(dummyUser.getId(), dummyAssessments.get(0).getAssessmentCategory())

        then:
        1 * userAssessmentRepository.getUserAssessments(dummyUser.getId(), dummyAssessments.get(0).getAssessmentCategory(), null) >> new Try.Success<List<UserAssessment>>([
                new CATUserAssessment(status: CompletionStatus.IN_PROGRESS, takenDate: takenDates.get(0)),
                new CATUserAssessment(status: CompletionStatus.GRADED, takenDate: takenDates.get(1)),
                new CATUserAssessment(status: CompletionStatus.GRADED, takenDate: takenDates.get(2))
        ])

        then:
        maybeTakenDates.isSuccess()
        maybeTakenDates.get().find{ it.getTakenDate() == takenDates.get(0) } == null //this one is in progress
        maybeTakenDates.get().find{ it.getTakenDate() == takenDates.get(1) } != null
        maybeTakenDates.get().find{ it.getTakenDate() == takenDates.get(2) } != null
    }

    def "getUserAssessmentTakenDates: getUserAssessments fails, i fail"(){
        when:
        Try<List<UserAssessmentTakenDate>> maybeTakenDates = userAssessmentService.getTakenDates(dummyUser.getId(), dummyAssessments.get(0).getAssessmentCategory())

        then:
        1 * userAssessmentRepository.getUserAssessments(dummyUser.getId(), dummyAssessments.get(0).getAssessmentCategory(), null) >> new Try.Failure<List<UserAssessment>>(new Exception())

        then:
        maybeTakenDates.isFailure()
    }

    def "saveWritingSample: success"(){
        setup:
        SaveWritingSampleRequest saveWritingSampleRequest = new SaveWritingSampleRequest(sample: "hey hey hey")
        WritingPromptUserAssessment latestUserAssessment = new WritingPromptUserAssessment(status: CompletionStatus.IN_PROGRESS, assessmentType: AssessmentType.WRITING_PROMPT)

        when:
        Try<WritingPrompt> maybeWritingSample = userAssessmentService.saveWritingSample(dummyUser.getId(), dummyAssessments.get(2).getId(), saveWritingSampleRequest)

        then:
        1 * userAssessmentRepository.getLatestUserAssessment(dummyUser.getId(), dummyAssessments.get(2).getId()) >> new Try.Success<UserAssessment>(latestUserAssessment);
        1 * assessmentRepository.getAssessment(dummyAssessments.get(2).getId()) >> new Try.Success<Assessment>(dummyAssessments.get(3));

        then:
        1 * userAssessmentRepository.saveUserAssessment(_) >> { arguments ->
            WritingPromptUserAssessment userAssessmentParam = arguments[0]
            userAssessmentParam.writingPrompt.sample == saveWritingSampleRequest.sample

            return new Try.Success<Void>(null)
        }

        then:
        maybeWritingSample.isSuccess()
    }

    @Unroll
    def "saveWritingSample: failure if not in progress"(CompletionStatus completionStatus, boolean isSuccess){
        setup:
        SaveWritingSampleRequest saveWritingSampleRequest = new SaveWritingSampleRequest(sample: "hey hey hey")
        WritingPromptUserAssessment latestUserAssessment = new WritingPromptUserAssessment(status: completionStatus, assessmentType: AssessmentType.WRITING_PROMPT)
        userAssessmentRepository.getLatestUserAssessment(*_) >> new Try.Success<UserAssessment>(latestUserAssessment)
        userAssessmentRepository.saveUserAssessment(_) >> new Try.Success<Void>(null)
        assessmentRepository.getAssessment(dummyAssessments.get(2).getId()) >> new Try.Success<Assessment>(dummyAssessments.get(3))

        when:
        Try<WritingPrompt> maybeWritingSample = userAssessmentService.saveWritingSample(dummyUser.getId(), dummyAssessments.get(2).getId(), saveWritingSampleRequest)

        then:
        maybeWritingSample.isSuccess() == isSuccess

        where:
        completionStatus             | isSuccess
        CompletionStatus.COMPLETED   | false
        CompletionStatus.GRADED      | false
        CompletionStatus.IN_PROGRESS | true
    }

    @Unroll
    def "saveWritingSample: failure if not writing prompt"(AssessmentType assessmentType, boolean isSuccess){
        setup:
        SaveWritingSampleRequest saveWritingSampleRequest = new SaveWritingSampleRequest(sample: "hey hey hey")
        WritingPromptUserAssessment latestUserAssessment = new WritingPromptUserAssessment(status: CompletionStatus.IN_PROGRESS, assessmentType: assessmentType)
        userAssessmentRepository.getLatestUserAssessment(*_) >> new Try.Success<UserAssessment>(latestUserAssessment)
        userAssessmentRepository.saveUserAssessment(_) >> new Try.Success<Void>(null)
        assessmentRepository.getAssessment(dummyAssessments.get(2).getId()) >> new Try.Success<Assessment>(dummyAssessments.get(3))

        when:
        Try<WritingPrompt> maybeWritingSample = userAssessmentService.saveWritingSample(dummyUser.getId(), dummyAssessments.get(2).getId(), saveWritingSampleRequest)

        then:
        maybeWritingSample.isSuccess() == isSuccess

        where:
        assessmentType                 | isSuccess
        AssessmentType.CAT             | false
        AssessmentType.LIKERT          | false
        AssessmentType.MULTIPLE_CHOICE | false
        AssessmentType.WRITING_PROMPT  | true
    }


    def "saveWritingSample: getLatestUserAssessment fails, i fail"(){
        when:
        Try<WritingPrompt> maybeWritingSample = userAssessmentService.saveWritingSample(dummyUser.getId(), dummyAssessments.get(2).getId(), new SaveWritingSampleRequest())

        then:
        1 * userAssessmentRepository.getLatestUserAssessment(*_) >> new Try.Failure<UserAssessment>(new Exception());
        0 * userAssessmentRepository.saveUserAssessment(_)

        then:
        maybeWritingSample.isFailure()
    }

    def "saveWritingSample: saveUserAssessment fails, i fail"(){
        setup:
        WritingPromptUserAssessment latestUserAssessment = new WritingPromptUserAssessment(status: CompletionStatus.IN_PROGRESS, assessmentType: AssessmentType.WRITING_PROMPT)

        when:
        Try<WritingPrompt> maybeWritingSample = userAssessmentService.saveWritingSample(dummyUser.getId(), dummyAssessments.get(2).getId(), new SaveWritingSampleRequest())

        then:
        1 * userAssessmentRepository.getLatestUserAssessment(dummyUser.getId(), dummyAssessments.get(2).getId()) >> new Try.Success<UserAssessment>(latestUserAssessment);
        1 * assessmentRepository.getAssessment(dummyAssessments.get(2).getId()) >> new Try.Success<Assessment>(dummyAssessments.get(3));

        then:
        1 * userAssessmentRepository.saveUserAssessment(_) >> new Try.Failure<Void>(null)

        then:
        maybeWritingSample.isFailure()
    }

    def "saveWritingSample: getAssessment fails, i fail"(){
        setup:
        WritingPromptUserAssessment latestUserAssessment = new WritingPromptUserAssessment(status: CompletionStatus.IN_PROGRESS, assessmentType: AssessmentType.WRITING_PROMPT)

        when:
        Try<WritingPrompt> maybeWritingSample = userAssessmentService.saveWritingSample(dummyUser.getId(), dummyAssessments.get(2).getId(), new SaveWritingSampleRequest())

        then:
        1 * userAssessmentRepository.getLatestUserAssessment(dummyUser.getId(), dummyAssessments.get(2).getId()) >> new Try.Success<UserAssessment>(latestUserAssessment);
        1 * assessmentRepository.getAssessment(dummyAssessments.get(2).getId()) >> new Try.Failure<Assessment>(new Exception());
        0 * userAssessmentRepository.saveUserAssessment(_)

        then:
        maybeWritingSample.isFailure()
    }

    def "getLatestSummary: returns mapped assessment summary"(){
        when:
        Try<UserAssessmentSummary> maybeUserAssessmentSummary = userAssessmentService.getLatestSummary(dummyUser.id, dummyAssessmentId)

        then:
        1 * userAssessmentRepository.getLatestUserAssessment(dummyUser.id, dummyAssessmentId) >> new Try.Success<UserAssessment>(dummyUserAssessments.get(0))

        then:
        maybeUserAssessmentSummary.isSuccess()
        maybeUserAssessmentSummary.get().assessmentId == dummyUserAssessments.get(0).assessmentId
    }

    def "getLatestSummary: repo call fails, i fail"(){
        when:
        Try<UserAssessmentSummary> maybeUserAssessmentSummary = userAssessmentService.getLatestSummary(dummyUser.id, dummyAssessmentId)

        then:
        1 * userAssessmentRepository.getLatestUserAssessment(dummyUser.id, dummyAssessmentId) >> new Try.Failure<UserAssessment>(new Exception())

        then:
        maybeUserAssessmentSummary.isFailure()
    }

    def "getLatestSummary 2: returns mapped assessment summary"(){
        when:
        Try<UserAssessmentSummary> maybeUserAssessmentSummary = userAssessmentService.getLatestSummary(dummyUser.id, AssessmentCategory.MATHEMATICS)

        then:
        1 * userAssessmentRepository.getLatestUserAssessment(dummyUser.id, AssessmentCategory.MATHEMATICS) >> new Try.Success<UserAssessment>(dummyUserAssessments.get(0))

        then:
        maybeUserAssessmentSummary.isSuccess()
        maybeUserAssessmentSummary.get().assessmentId == dummyUserAssessments.get(0).assessmentId
    }

    def "getLatestSummary 2: repo call fails, i fail"(){
        when:
        Try<UserAssessmentSummary> maybeUserAssessmentSummary = userAssessmentService.getLatestSummary(dummyUser.id, AssessmentCategory.MATHEMATICS)

        then:
        1 * userAssessmentRepository.getLatestUserAssessment(dummyUser.id, AssessmentCategory.MATHEMATICS) >> new Try.Failure<UserAssessment>(new Exception())

        then:
        maybeUserAssessmentSummary.isFailure()
    }

    def "gradeUserAssessments - graded: success"(){

        setup:
        AssessmentCategory[] assessmentCategories = null;
        Instant startDate = Instant.parse("2016-01-01T00:00:00.000Z")
        Instant endDate = Instant.parse("2016-01-01T00:00:00.000Z")
        boolean dryRun = false
        CompletionStatus completionStatus = CompletionStatus.GRADED
        UpdateUserAssessmentRequest updateUserAssessmentRequest = new UpdateUserAssessmentRequest(id: "abc", status: CompletionStatus.COMPLETED);
        UserAssessment userAssessment = new MultipleChoiceUserAssessment()
        userAssessment.setId("abc")
        userAssessment.setAssessmentType(AssessmentType.MULTIPLE_CHOICE)
        userAssessment.setAssessmentId("assessmentid")
        userAssessment.setUserId("userid")

        when:
        Try<List<UserAssessment>> maybeUserAssessment = userAssessmentService.gradeUserAssessments(assessmentCategories, completionStatus, startDate, endDate, dryRun)

        then:
        1 * userAssessmentRepository.getUserAssessments(assessmentCategories, completionStatus, startDate, endDate) >> new Try.Success<List<UserAssessment>>([userAssessment])

        then:
        1 * messageService.queueUserAssessmentForGrading(_) >> new Try.Success<Void>(null)
        maybeUserAssessment.isSuccess()
    }

    def "gradeUserAssessments - ungraded: success"(){

        setup:
        AssessmentCategory[] assessmentCategories = null;
        Instant startDate = Instant.parse("2016-01-01T00:00:00.000Z");
        Instant endDate = Instant.parse("2016-01-01T00:00:00.000Z");
        boolean dryRun = false;
        CompletionStatus completionStatus = CompletionStatus.COMPLETED;
        UpdateUserAssessmentRequest updateUserAssessmentRequest = new UpdateUserAssessmentRequest(id: "abc", status: CompletionStatus.COMPLETED);
        UserAssessment userAssessment = new MultipleChoiceUserAssessment()
        userAssessment.setId("abc")
        userAssessment.setAssessmentType(AssessmentType.MULTIPLE_CHOICE)
        userAssessment.setAssessmentId("assessmentid")
        userAssessment.setUserId("userid")

        when:
        Try<List<UserAssessment>> maybeUserAssessment = userAssessmentService.gradeUserAssessments(assessmentCategories, completionStatus, startDate, endDate, dryRun)

        then:
        1 * userAssessmentRepository.getUserAssessments(assessmentCategories, completionStatus, startDate, endDate) >> new Try.Success<List<UserAssessment>>([userAssessment])

        then:
        1 * userAssessmentRepository.getUserAssessmentById(_, "abc") >> new Try.Success<UserAssessment>(userAssessment)
        1 * scoringService.canAutoGradeFromUpdate(userAssessment) >> true
        1 * scoringService.autoGradeUserAssessment(userAssessment) >> new Try.Success<UserAssessment>(userAssessment)
        0 * scoringService.manualGradeUserAssessment(*_)
        1 * userAssessmentRepository.saveUserAssessment(userAssessment) >> new Try.Success<Void>(null)
        1 * messageService.queueCanvasSubmissionUpdate(userAssessment.getUserId()) >> new Try.Success<Void>(null)
        maybeUserAssessment.isSuccess()
    }

    def "gradeUserAssessments: get user assessment fail"(){

        setup:
        AssessmentCategory[] assessmentCategories = null;
        Instant startDate = Instant.parse("2016-01-01T00:00:00.000Z");
        Instant endDate = Instant.parse("2016-01-01T00:00:00.000Z");
        boolean dryRun = false;
        CompletionStatus completionStatus = CompletionStatus.COMPLETED;

        when:
        Try<List<UserAssessment>> maybeUserAssessment = userAssessmentService.gradeUserAssessments(assessmentCategories, completionStatus, startDate, endDate, dryRun)

        then:
        1 * userAssessmentRepository.getUserAssessments(assessmentCategories, completionStatus, startDate, endDate) >> new Try.Failure<List<UserAssessment>>(new Exception())

        then:
        maybeUserAssessment.isFailure()
    }

    def "gradeUserAssessments: queue fail"(){

        setup:
        AssessmentCategory[] assessmentCategories = null;
        Instant startDate = Instant.parse("2016-01-01T00:00:00.000Z");
        Instant endDate = Instant.parse("2016-01-01T00:00:00.000Z");
        boolean dryRun = false;
        CompletionStatus completionStatus = CompletionStatus.GRADED;

        when:
        Try<List<UserAssessment>> maybeUserAssessment = userAssessmentService.gradeUserAssessments(assessmentCategories, completionStatus, startDate, endDate, dryRun)

        then:
        1 * userAssessmentRepository.getUserAssessments(assessmentCategories, completionStatus, startDate, endDate) >> new Try.Success<List<UserAssessment>>(dummyUserAssessments)

        then:
        1 * messageService.queueUserAssessmentForGrading(_) >> new Try.Failure<Void>(null)
        maybeUserAssessment.isFailure()
    }

    def "updateUserAssessment: autograde success"(){
        setup:
        UpdateUserAssessmentRequest updateUserAssessmentRequest = new UpdateUserAssessmentRequest(id: dummyUserAssessmentId, status: CompletionStatus.COMPLETED)

        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentService.updateUserAssessment(dummyUser.getId(), dummyUser.getRoles(), updateUserAssessmentRequest)

        then:
        1 * userAssessmentRepository.getUserAssessmentById(dummyUser.getId(), updateUserAssessmentRequest.getId()) >> new Try.Success<UserAssessment>(dummyUserAssessments.get(0))

        then:
        1 * scoringService.canAutoGradeFromUpdate(dummyUserAssessments.get(0)) >> true

        then:
        1 * scoringService.autoGradeUserAssessment(dummyUserAssessments.get(0)) >> new Try.Success<UserAssessment>(dummyUserAssessments.get(0))
        0 * scoringService.manualGradeUserAssessment(*_)

        then:
        1 * userAssessmentRepository.saveUserAssessment(dummyUserAssessments.get(0)) >> new Try.Success<Void>(null)
        1 * messageService.queueCanvasSubmissionUpdate(dummyUser.getId()) >> new Try.Success<Void>(null)
        maybeUserAssessment.isSuccess()
    }


    def "updateUserAssessment: autograde failed, successfully put on queue"(){
        setup:
        UpdateUserAssessmentRequest updateUserAssessmentRequest = new UpdateUserAssessmentRequest(id: dummyUserAssessmentId, status: CompletionStatus.COMPLETED)

        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentService.updateUserAssessment(dummyUser.getId(), dummyUser.getRoles(), updateUserAssessmentRequest)

        then:
        1 * userAssessmentRepository.getUserAssessmentById(dummyUser.getId(), updateUserAssessmentRequest.getId()) >> new Try.Success<UserAssessment>(dummyUserAssessments.get(0))

        then:
        1 * scoringService.canAutoGradeFromUpdate(dummyUserAssessments.get(0)) >> true
        1 * scoringService.autoGradeUserAssessment(dummyUserAssessments.get(0)) >> new Try.Failure<UserAssessment>(new Exception())

        then:
        1 * userAssessmentRepository.saveUserAssessment(_) >> { args ->
            UserAssessment savingUserAssessment = args[0]
            assert savingUserAssessment.getStatus() == CompletionStatus.COMPLETED
            return new Try.Success<Void>(null)
        }
        1 * messageService.queueUserAssessmentForGrading(_) >> new Try.Success<Void>(null)
        1 * messageService.queueCanvasSubmissionUpdate(dummyUser.getId()) >> new Try.Success<Void>(null)

        then:
        maybeUserAssessment.isSuccess()
        maybeUserAssessment.get().status == CompletionStatus.COMPLETED
    }

    def "updateUserAssessment: autograde failed, getUserAssessmentById fails, i fail"(){
        setup:
        UpdateUserAssessmentRequest updateUserAssessmentRequest = new UpdateUserAssessmentRequest(id: dummyUserAssessmentId, status: CompletionStatus.COMPLETED)

        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentService.updateUserAssessment(dummyUser.getId(), dummyUser.getRoles(), updateUserAssessmentRequest)

        then:
        1 * userAssessmentRepository.getUserAssessmentById(dummyUser.getId(), updateUserAssessmentRequest.getId()) >> new Try.Failure<UserAssessment>(new Exception())
        0 * scoringService.canAutoGradeFromUpdate(_)
        0 * scoringService.autoGradeUserAssessment(_)
        0 * userAssessmentRepository.saveUserAssessment(_)
        0 * messageService.queueUserAssessmentForGrading(_)

        then:
        maybeUserAssessment.isFailure()
    }

    def "updateUserAssessment: autograde failed, saveUserAssessment fails, i fail"(){
        setup:
        UpdateUserAssessmentRequest updateUserAssessmentRequest = new UpdateUserAssessmentRequest(id: dummyUserAssessmentId, status: CompletionStatus.COMPLETED)

        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentService.updateUserAssessment(dummyUser.getId(), dummyUser.getRoles(), updateUserAssessmentRequest)

        then:
        1 * userAssessmentRepository.getUserAssessmentById(dummyUser.getId(), updateUserAssessmentRequest.getId()) >> new Try.Success<UserAssessment>(dummyUserAssessments.get(0))
        1 * scoringService.canAutoGradeFromUpdate(dummyUserAssessments.get(0)) >> true

        then:
        1 * scoringService.autoGradeUserAssessment(dummyUserAssessments.get(0)) >> new Try.Failure<UserAssessment>(new Exception())

        then:
        1 * userAssessmentRepository.saveUserAssessment(_) >> new Try.Failure<Void>(null)
        0 * messageService.queueUserAssessmentForGrading(_)

        then:
        maybeUserAssessment.isFailure()
    }

    def "updateUserAssessment: autograde failed, queueUserAssessmentForGrading fails, i fail"(){
        setup:
        UpdateUserAssessmentRequest updateUserAssessmentRequest = new UpdateUserAssessmentRequest(id: dummyUserAssessmentId, status: CompletionStatus.COMPLETED)

        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentService.updateUserAssessment(dummyUser.getId(), dummyUser.getRoles(), updateUserAssessmentRequest)

        then:
        1 * userAssessmentRepository.getUserAssessmentById(dummyUser.getId(), updateUserAssessmentRequest.getId()) >> new Try.Success<UserAssessment>(dummyUserAssessments.get(0))
        1 * scoringService.canAutoGradeFromUpdate(dummyUserAssessments.get(0)) >> true

        then:
        1 * scoringService.autoGradeUserAssessment(dummyUserAssessments.get(0)) >> new Try.Failure<UserAssessment>(new Exception())

        then:
        1 * userAssessmentRepository.saveUserAssessment(_) >> new Try.Success<Void>(null)
        1 * messageService.queueUserAssessmentForGrading(_) >> new Try.Failure<Void>(null)

        then:
        maybeUserAssessment.isFailure()
    }

    def "updateUserAssessment: manual grade success"(){
        setup:
        dummyUser.roles.add("ROLE_ADMIN")

        UpdateUserAssessmentRequest updateUserAssessmentRequest = new UpdateUserAssessmentRequest(
                id: dummyUserAssessmentId,
                userId: UUID.randomUUID().toString(),
                status: CompletionStatus.GRADED,
                domainScores: [new DomainScore(domainId: "domain-1", rubricScore: CompletionScore.MEDIUM)],
                overallScore: CompletionScore.MEDIUM)

        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentService.updateUserAssessment(dummyUser.getId(), dummyUser.getRoles(), updateUserAssessmentRequest)

        then:
        0 * scoringService.autoGradeUserAssessment(*_)
        1 * scoringService.manualGradeUserAssessment(
                updateUserAssessmentRequest.getUserId(),
                dummyUserAssessmentId,
                updateUserAssessmentRequest.getDomainScores(),
                updateUserAssessmentRequest.getOverallScore()) >> new Try.Success<UserAssessment>(dummyUserAssessments.get(0))

        then:
        1 * userAssessmentRepository.saveUserAssessment(dummyUserAssessments.get(0)) >> new Try.Success<Void>(null)
        1 * messageService.queueCanvasSubmissionUpdate(dummyUser.getId()) >> new Try.Success<Void>(null)
        maybeUserAssessment.isSuccess()
    }

    def "updateUserAssessment: manual grade bad permissions"(){
        setup:
        UpdateUserAssessmentRequest updateUserAssessmentRequest = new UpdateUserAssessmentRequest(status: CompletionStatus.GRADED)

        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentService.updateUserAssessment(dummyUser.getId(), dummyUser.getRoles(), updateUserAssessmentRequest)

        then:
        0 * scoringService.autoGradeUserAssessment(*_)
        0 * scoringService.manualGradeUserAssessment(*_)

        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof InsufficientPermissionsException
    }

    def "updateUserAssessment: bad request #1"(){
        setup:
        dummyUser.roles.add("ROLE_ADMIN")
        UpdateUserAssessmentRequest updateUserAssessmentRequest = new UpdateUserAssessmentRequest(
                status: CompletionStatus.GRADED,
                userId: null,
                domainScores: [],
                overallScore: CompletionScore.MEDIUM)

        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentService.updateUserAssessment(dummyUser.getId(), dummyUser.getRoles(), updateUserAssessmentRequest)

        then:
        0 * scoringService.autoGradeUserAssessment(*_)
        0 * scoringService.manualGradeUserAssessment(*_)

        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof BadInputException
    }

    def "updateUserAssessment: bad request #2"(){
        setup:
        dummyUser.roles.add("ROLE_ADMIN")
        UpdateUserAssessmentRequest updateUserAssessmentRequest = new UpdateUserAssessmentRequest(
                status: CompletionStatus.GRADED,
                userId: dummyUser.getId(),
                domainScores: [],
                overallScore: null)

        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentService.updateUserAssessment(dummyUser.getId(), dummyUser.getRoles(), updateUserAssessmentRequest)

        then:
        0 * scoringService.autoGradeUserAssessment(*_)
        0 * scoringService.manualGradeUserAssessment(*_)

        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof BadInputException
    }

    def "updateUserAssessment: bad request #3"(){
        setup:
        dummyUser.roles.add("ROLE_ADMIN")
        UpdateUserAssessmentRequest updateUserAssessmentRequest = new UpdateUserAssessmentRequest(
                status: CompletionStatus.GRADED,
                userId: dummyUser.getId(),
                domainScores: null,
                overallScore: CompletionScore.MEDIUM)

        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentService.updateUserAssessment(dummyUser.getId(), dummyUser.getRoles(), updateUserAssessmentRequest)

        then:
        0 * scoringService.autoGradeUserAssessment(*_)
        0 * scoringService.manualGradeUserAssessment(*_)

        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof BadInputException
    }

    def "getUserAssessment: success"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentService.getUserAssesment(dummyUser.getId(), dummyUserAssessments.get(0).getId())

        then:
        1 * userAssessmentRepository.getUserAssessmentById(dummyUser.getId(), dummyUserAssessments.get(0).getId()) >> new Try.Success<UserAssessment>(dummyUserAssessments.get(0))
        maybeUserAssessment.isSuccess()
    }

    def "getUserAssessment: getUserAssessmentById fails, i fail"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentService.getUserAssesment(dummyUser.getId(), dummyUserAssessments.get(0).getId())

        then:
        1 * userAssessmentRepository.getUserAssessmentById(dummyUser.getId(), dummyUserAssessments.get(0).getId()) >> new Try.Failure<UserAssessment>(new Exception())
        maybeUserAssessment.isFailure()
    }

    def "saveUserAssessment: success"(){
        when:
        Try<Void> maybeSaved = userAssessmentService.saveUserAssessment(dummyUserAssessments.get(0))

        then:
        1 * userAssessmentRepository.saveUserAssessment(dummyUserAssessments.get(0)) >> new Try.Success<Void>(null)
        maybeSaved.isSuccess()
    }

    def "saveUserAssessment: getUserAssessmentById fails, i fail"(){
        when:
        Try<Void> maybeSaved = userAssessmentService.saveUserAssessment(dummyUserAssessments.get(0))

        then:
        1 * userAssessmentRepository.saveUserAssessment(dummyUserAssessments.get(0)) >> new Try.Failure<Void>(null)
        maybeSaved.isFailure()
    }

    def "getWritingPrompt: success"(){
        when:
        Try<WritingPrompt> maybeWritingPrompt = userAssessmentService.getWritingPrompt(dummyUser.getId(), dummyUserAssessments.get(2).getId())

        then:
        1 * userAssessmentRepository.getLatestUserAssessment(dummyUser.getId(), dummyUserAssessments.get(2).getId()) >> new Try.Success<UserAssessment>(dummyUserAssessments.get(2));

        then:
        maybeWritingPrompt.isSuccess()
        maybeWritingPrompt.get().sample == ((WritingPromptUserAssessment)dummyUserAssessments.get(2)).getWritingPrompt().sample
    }

    def "getWritingPrompt: getLatestUserAssessment fails, i fail"(){
        when:
        Try<WritingPrompt> maybeWritingPrompt = userAssessmentService.getWritingPrompt(dummyUser.getId(), dummyUserAssessments.get(2).getId())

        then:
        1 * userAssessmentRepository.getLatestUserAssessment(dummyUser.getId(), dummyUserAssessments.get(2).getId()) >> new Try.Failure<UserAssessment>(new Exception());

        then:
        maybeWritingPrompt.isFailure()
    }

    def "getWritingPrompt: user assessment is not WRITING_PROMPT, i fail"(){
        when:
        Try<WritingPrompt> maybeWritingPrompt = userAssessmentService.getWritingPrompt(dummyUser.getId(), dummyUserAssessments.get(0).getId())

        then:
        1 * userAssessmentRepository.getLatestUserAssessment(dummyUser.getId(), dummyUserAssessments.get(0).getId()) >> new Try.Success<UserAssessment>(dummyUserAssessments.get(0));

        then:
        maybeWritingPrompt.isFailure()
        maybeWritingPrompt.failed().get() instanceof IncompatibleTypeException
    }

    def "getWritingPrompt: user assessment is not IN_PROGRESS, i fail"(){
        setup:
        dummyUserAssessments.get(2).setStatus(CompletionStatus.COMPLETED)

        when:
        Try<WritingPrompt> maybeWritingPrompt = userAssessmentService.getWritingPrompt(dummyUser.getId(), dummyUserAssessments.get(2).getId())

        then:
        1 * userAssessmentRepository.getLatestUserAssessment(dummyUser.getId(), dummyUserAssessments.get(2).getId()) >> new Try.Success<UserAssessment>(dummyUserAssessments.get(2));

        then:
        maybeWritingPrompt.isFailure()
        maybeWritingPrompt.failed().get() instanceof IncompatibleStatusException
    }

    def "getWritingPrompt: success retrieving from Assessment"(){
        when:
        Try<WritingPrompt> maybeWritingPrompt = userAssessmentService.getWritingPrompt(dummyUser.getId(), dummyUserAssessments.get(3).getId())

        then:
        1 * userAssessmentRepository.getLatestUserAssessment(dummyUser.getId(), dummyUserAssessments.get(3).getId()) >> new Try.Success<UserAssessment>(dummyUserAssessments.get(3));
        1 * assessmentRepository.getAssessment(dummyUserAssessments.get(3).getId()) >> new Try.Success<Assessment>(dummyAssessments.get(3))

        then:
        maybeWritingPrompt.isSuccess()
        maybeWritingPrompt.get().sample == ((WritingAssessment)dummyAssessments.get(3)).getWritingPrompt().sample
    }

    def "getWritingPrompt: getAssessment fails, i fail"(){
        when:
        Try<WritingPrompt> maybeWritingPrompt = userAssessmentService.getWritingPrompt(dummyUser.getId(), dummyUserAssessments.get(3).getId())

        then:
        1 * userAssessmentRepository.getLatestUserAssessment(dummyUser.getId(), dummyUserAssessments.get(3).getId()) >> new Try.Success<UserAssessment>(dummyUserAssessments.get(3));
        1 * assessmentRepository.getAssessment(*_) >> new Try.Failure<Assessment>(new Exception())

        then:
        maybeWritingPrompt.isFailure()
    }

    def "getSummaries (by status): returns mapped assessment summaries"(){
        setup:
        List<CompletionStatus> statuses = [CompletionStatus.COMPLETED];
        List<ScoringType> scoringTypes = [ScoringType.AVERAGE, ScoringType.SUM];

        when:
        Try<List<UserAssessmentSummary>> maybeUserAssessmentSummaries = userAssessmentService.getSummaries(statuses, scoringTypes, dummyUser.id, 10, 0);

        then:
        1 * userAssessmentRepository.getUserAssessments(statuses, scoringTypes, dummyUser.id, 10, 0) >> new Try.Success<List<UserAssessment>>(dummyUserAssessments)

        then:
        maybeUserAssessmentSummaries.isSuccess()
        maybeUserAssessmentSummaries.get().get(0).assessmentId == dummyUserAssessments.get(0).assessmentId
        maybeUserAssessmentSummaries.get().get(0).completionDate == dummyUserAssessments.get(0).completionDate
        maybeUserAssessmentSummaries.get().get(0).overallScore == dummyUserAssessments.get(0).overallScore
        maybeUserAssessmentSummaries.get().get(0).domainScores.get(0).rubricScore == dummyUserAssessments.get(0).domainScores.get(0).rubricScore
        maybeUserAssessmentSummaries.get().get(0).domainScores.get(0).domainId == dummyUserAssessments.get(0).domainScores.get(0).domainId
        maybeUserAssessmentSummaries.get().get(0).progressPercentage == dummyUserAssessments.get(0).progressPercentage
        maybeUserAssessmentSummaries.get().get(0).status == dummyUserAssessments.get(0).status
        maybeUserAssessmentSummaries.get().get(0).takenDate == dummyUserAssessments.get(0).takenDate
    }

    def "getSummaries (by status): repo call fails, i fail"(){
        setup:
        List<CompletionStatus> statuses = [CompletionStatus.COMPLETED];
        List<ScoringType> scoringTypes = [ScoringType.AVERAGE, ScoringType.SUM];

        when:
        Try<List<UserAssessmentSummary>> maybeUserAssessmentSummaries = userAssessmentService.getSummaries(statuses, scoringTypes, dummyUser.id, 10, 0);

        then:
        1 * userAssessmentRepository.getUserAssessments(*_) >> new Try.Failure<List<UserAssessment>>(new Exception())

        then:
        maybeUserAssessmentSummaries.isFailure()
    }

    def "getUserAssessmentsForManualGrading: success"(){
        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentService.getUserAssessmentsForManualGrading()

        then:
        1 * assessmentRepository.getAssessments([ScoringType.MANUAL], true) >> new Try.Success<List<Assessment>>([dummyAssessments.get(3)])
        1 * userAssessmentRepository.getUserAssessments([CompletionStatus.COMPLETED], [dummyAssessments.get(3).getId()]) >> new Try.Success<List<UserAssessment>>([dummyUserAssessments.get(4)])

        then:
        maybeUserAssessments.isSuccess()
    }

    def "getUserAssessmentsForManualGrading: getUserAssessments fails, i fail"(){
        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentService.getUserAssessmentsForManualGrading()

        then:
        1 * assessmentRepository.getAssessments([ScoringType.MANUAL], true) >> new Try.Success<List<Assessment>>([dummyAssessments.get(3)])
        1 * userAssessmentRepository.getUserAssessments([CompletionStatus.COMPLETED], [dummyAssessments.get(3).getId()]) >> new Try.Failure<List<UserAssessment>>(new Exception())

        then:
        maybeUserAssessments.isFailure()
    }

    def "getUserAssessmentsForManualGrading: getAssessments fails, i fail"(){
        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentService.getUserAssessmentsForManualGrading()

        then:
        1 * assessmentRepository.getAssessments([ScoringType.MANUAL], true) >> new Try.Failure<List<Assessment>>(new Exception())
        0 * userAssessmentRepository.getUserAssessments(*_)

        then:
        maybeUserAssessments.isFailure()
    }

    def "getCompletedUserAssessmentSummaries: success"(){
        setup:
        Instant startDate = Instant.parse("2016-01-01T00:00:00.000Z");
        Instant endDate = Instant.parse("2016-01-01T00:00:00.000Z");

        when:
        Try<List<UserAssessmentSummary>> maybeUserAssessmentSummaries = userAssessmentService.getCompletedUserAssessmentSummaries(startDate, endDate);

        then:
        1 * userAssessmentRepository.getCompletedUserAssessments(startDate, endDate) >> new Try.Success<List<UserAssessment>>([new CATUserAssessment()])

        then:
        maybeUserAssessmentSummaries.isSuccess()
        maybeUserAssessmentSummaries.get().size() == 1
    }

    def "getCompletedUserAssessmentSummaries: getCompletedUserAssessments fails, i fail"(){
        setup:
        Instant startDate = Instant.parse("2016-01-01T00:00:00.000Z");
        Instant endDate = Instant.parse("2016-01-01T00:00:00.000Z");

        when:
        Try<List<UserAssessmentSummary>> maybeUserAssessmentSummaries = userAssessmentService.getCompletedUserAssessmentSummaries(startDate, endDate);

        then:
        1 * userAssessmentRepository.getCompletedUserAssessments(*_) >> new Try.Failure<List<UserAssessment>>(new Exception())

        then:
        maybeUserAssessmentSummaries.isFailure()
    }

    def "getCompletionSummary: has completed all"(){
        when:
        Try<CompletionSummary> maybeResults = userAssessmentService.getCompletionSummary(dummyUser.getId())

        then:
        1 * userAssessmentRepository.getUserAssessments(Arrays.asList(CompletionStatus.COMPLETED, CompletionStatus.GRADED, CompletionStatus.GRADING_FAILURE), null, dummyUser.getId(), null, null) >> new Try.Success<List<UserAssessment>>([
                new CATUserAssessment(assessmentCategory: AssessmentCategory.MATHEMATICS),
                new WritingPromptUserAssessment(assessmentCategory: AssessmentCategory.WRITING),
                new MultipleChoiceUserAssessment(assessmentCategory: AssessmentCategory.COLLEGE_SKILLS),
                new MultipleChoiceUserAssessment(assessmentCategory: AssessmentCategory.READING)
        ])

        then:
        maybeResults.isSuccess()
        maybeResults.get().hasCompletedAllCategories
    }

    def "getCompletionSummary: has not completed all"(){
        when:
        Try<CompletionSummary> maybeResults = userAssessmentService.getCompletionSummary(dummyUser.getId())

        then:
        1 * userAssessmentRepository.getUserAssessments(Arrays.asList(CompletionStatus.COMPLETED, CompletionStatus.GRADED, CompletionStatus.GRADING_FAILURE), null, dummyUser.getId(), null, null) >> new Try.Success<List<UserAssessment>>([
                new CATUserAssessment(assessmentCategory: AssessmentCategory.MATHEMATICS),
                new WritingPromptUserAssessment(assessmentCategory: AssessmentCategory.WRITING),
                new MultipleChoiceUserAssessment(assessmentCategory: AssessmentCategory.READING)
        ])

        then:
        maybeResults.isSuccess()
        !maybeResults.get().hasCompletedAllCategories
    }

    def "getCompletionSummary: getUserAssessments fails, i fail"(){
        when:
        Try<CompletionSummary> maybeResults = userAssessmentService.getCompletionSummary(dummyUser.getId())

        then:
        1 * userAssessmentRepository.getUserAssessments(*_) >> new Try.Failure<List<UserAssessment>>(new Exception())

        then:
        maybeResults.isFailure()
    }
}
