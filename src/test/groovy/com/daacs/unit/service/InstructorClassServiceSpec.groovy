package com.daacs.unit.service

import com.daacs.framework.exception.NotFoundException
import com.daacs.framework.hystrix.FailureType
import com.daacs.framework.hystrix.FailureTypeException
import com.daacs.framework.serializer.DaacsOrikaMapper
import com.daacs.model.StudentClassInvite
import com.daacs.model.ClassScoreResults
import com.daacs.model.User
import com.daacs.model.InstructorClass
import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.AssessmentCategoryGroup
import com.daacs.model.assessment.AssessmentType
import com.daacs.model.assessment.CATAssessment
import com.daacs.model.assessment.MultipleChoiceAssessment
import com.daacs.model.assessment.ScoringDomain
import com.daacs.model.assessment.WritingAssessment
import com.daacs.model.assessment.user.CATUserAssessment
import com.daacs.model.assessment.user.CompletionScore
import com.daacs.model.assessment.user.CompletionStatus
import com.daacs.model.assessment.user.DomainScore
import com.daacs.model.assessment.user.MultipleChoiceUserAssessment
import com.daacs.model.assessment.user.UserAssessment
import com.daacs.model.assessment.user.WritingPromptUserAssessment

import com.daacs.model.dto.InstructorClassUserScore
import com.daacs.model.dto.SendClassInviteRequest
import com.daacs.model.item.CATItemGroup
import com.daacs.model.item.Difficulty
import com.daacs.model.item.Item
import com.daacs.model.item.ItemAnswer
import com.daacs.model.item.ItemGroup
import com.daacs.model.item.WritingPrompt
import com.daacs.repository.InstructorClassRepository
import com.daacs.service.AssessmentService
import com.daacs.service.InstructorClassService
import com.daacs.service.InstructorClassServiceImpl
import com.daacs.service.MailService
import com.daacs.service.PendingStudentService
import com.daacs.service.UserAssessmentService
import com.daacs.service.UserService
import com.daacs.service.UserServiceImpl
import com.lambdista.util.Try
import com.opencsv.CSVWriter
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

import java.time.Instant

/**
 * Created by mgoldman on 6/9/20.
 */
class InstructorClassServiceSpec extends Specification {

    InstructorClassService instructorClassService
    UserService userService
    PendingStudentService pendingStudentService
    UserAssessmentService userAssessmentService
    AssessmentService assessmentService
    InstructorClassRepository classRepository
    MailService mailService
    DaacsOrikaMapper orikaMapper = new DaacsOrikaMapper()

    InstructorClass dummyClass = new InstructorClass(name: "testClass", instructorId: "123", assessmentIds: ["1", "2"], studentInvites: [], canEditAssessments: false);
    List<Assessment> dummyAssessments = [
            new CATAssessment(
                    id: "assessment-1",
                    assessmentType: AssessmentType.CAT,
                    assessmentCategoryGroup: new AssessmentCategoryGroup(id: "category_group_id"),
                    domains: [
                            new ScoringDomain(id: "domain", label: "Domain #1")
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

    FailureTypeException failureTypeException = new FailureTypeException("failure", "failure", FailureType.NOT_RETRYABLE, new NotFoundException(""))

    Instant dummyTakenDate = Instant.now();
    List<UserAssessment> dummyUserAssessments = [
            new CATUserAssessment(
                    assessmentId: "assessment-1",
                    assessmentType: AssessmentType.CAT,
                    assessmentCategoryGroupId: "category_group_id",
                    completionDate: Instant.now(),
                    overallScore: CompletionScore.MEDIUM,
                    progressPercentage: 1.0,
                    status: CompletionStatus.COMPLETED,
                    takenDate: dummyTakenDate,
                    domainScores: [new DomainScore(domainId: "domain", rubricScore: CompletionScore.MEDIUM)],
                    itemGroups: [
                            new CATItemGroup(difficulty: Difficulty.EASY, items: [
                                    new Item(
                                            question: "abc?",
                                            domainId: "domain",
                                            possibleItemAnswers: [new ItemAnswer(content: "abc", score: 1.0)],
                                            chosenItemAnswerId: null
                                    )
                            ]),
                            new CATItemGroup(difficulty: Difficulty.MEDIUM, items: [
                                    new Item(
                                            question: "def?",
                                            domainId: "domain",
                                            possibleItemAnswers: [new ItemAnswer(content: "def", score: 1.0)],
                                            chosenItemAnswerId: null
                                    )
                            ])]
            ),
            new MultipleChoiceUserAssessment(
                    assessmentId: "assessment-2",
                    assessmentType: AssessmentType.MULTIPLE_CHOICE,
                    assessmentCategoryGroupId: "multiple choice",
                    completionDate: Instant.now(),
                    overallScore: CompletionScore.MEDIUM,
                    progressPercentage: 1.0,
                    status: CompletionStatus.COMPLETED,
                    takenDate: dummyTakenDate,
                    domainScores: [new DomainScore(domainId: "domain", rubricScore: CompletionScore.MEDIUM)],
                    itemGroups: [
                            new ItemGroup(items: [
                                    new Item(
                                            question: "abc?",
                                            domainId: "domain",
                                            possibleItemAnswers: [new ItemAnswer(content: "abc", score: 1.0)],
                                            chosenItemAnswerId: null
                                    )
                            ]),
                            new ItemGroup(items: [
                                    new Item(
                                            question: "def?",
                                            domainId: "domain",
                                            possibleItemAnswers: [new ItemAnswer(content: "def", score: 1.0)],
                                            chosenItemAnswerId: null
                                    )
                            ])]
            ),
            new WritingPromptUserAssessment(
                    assessmentId: "assessment-3",
                    assessmentType: AssessmentType.WRITING_PROMPT,
                    assessmentCategoryGroupId: "writing",
                    completionDate: Instant.now(),
                    overallScore: CompletionScore.MEDIUM,
                    progressPercentage: 1.0,
                    status: CompletionStatus.IN_PROGRESS,
                    takenDate: dummyTakenDate,
                    domainScores: [new DomainScore(domainId: "domain", rubricScore: CompletionScore.MEDIUM)],
                    writingPrompt: new WritingPrompt(sample: "this is my writing sample")
            )
    ]

    def setup() {
        dummyClass.setId(UUID.randomUUID().toString())
        classRepository = Mock(InstructorClassRepository);
        userService = Mock(UserServiceImpl);
        pendingStudentService = Mock(PendingStudentService);
        userAssessmentService = Mock(UserAssessmentService);
        assessmentService = Mock(AssessmentService)
        mailService = Mock(MailService);
        instructorClassService = new InstructorClassServiceImpl(classRepository: classRepository, pendingStudentService:pendingStudentService, userService: userService, userAssessmentService: userAssessmentService, assessmentService: assessmentService, mailService: mailService, orikaMapper: orikaMapper);
    }

    def "getClass: returns instructorClass"() {
        setup:
        String instructorClassId = UUID.randomUUID().toString()

        when:
        Try<InstructorClass> maybeClass = instructorClassService.getClass(instructorClassId)

        then:
        1 * classRepository.getClass(instructorClassId) >> new Try.Success<InstructorClass>(dummyClass)
        maybeClass.isSuccess()
        maybeClass.get().id == dummyClass.id
    }

    def "getClass: has failure exception when instructorClass not found"() {
        setup:
        String instructorClassId = UUID.randomUUID().toString()

        when:
        Try<InstructorClass> maybeClass = instructorClassService.getClass(instructorClassId)

        then:
        1 * classRepository.getClass(_) >> new Try.Failure<InstructorClass>(failureTypeException)

        then:
        maybeClass.isFailure()
        maybeClass.failed().get().getCause() instanceof NotFoundException
    }

    def "saveClass: success"() {
        when:
        Try<InstructorClass> maybeClass = instructorClassService.saveClass(dummyClass)

        then:
        1 * classRepository.saveClass(dummyClass) >> new Try.Success<InstructorClass>(dummyClass)

        maybeClass.isSuccess()
        maybeClass.get() == dummyClass
    }

    def "saveClass: fails"() {
        when:
        Try<InstructorClass> maybeClass = instructorClassService.saveClass(dummyClass)

        then:
        1 * classRepository.saveClass(_) >> new Try.Failure<Class>(new Exception())

        maybeClass.isFailure()
    }


    def "updateClass: success"() {
        setup:
        InstructorClass updateClassRequest = new InstructorClass(name: "other_class_name", instructorId: "other_instructor_id", assessmentIds: ["1", "2"]);
        updateClassRequest.id = dummyClass.id

        when:
        Try<InstructorClass> maybeResults = instructorClassService.updateClass(updateClassRequest)

        then:
        1 * classRepository.getClass(dummyClass.getId()) >> new Try.Success<InstructorClass>(dummyClass)
        1 * userService.getInstructorById(updateClassRequest.getInstructorId()) >> new Try.Success<User>(new User())

        1 * classRepository.saveClass(_) >> { args ->
            InstructorClass instructorClass = args[0]
            assert instructorClass.id == dummyClass.id
            assert instructorClass.name == dummyClass.name

            dummyClass = instructorClass

            return new Try.Success<Void>(null)
        }

        then:
        maybeResults.isSuccess()
        maybeResults.get() == dummyClass
        maybeResults.get().name == updateClassRequest.name
    }

    def "updateClass: saveClass fails, i fail"() {
        setup:
        InstructorClass updateClassRequest = new InstructorClass(name: "other_class_name", instructorId: "123", assessmentIds: ["1", "2"]);
        updateClassRequest.id = dummyClass.id

        when:
        Try<InstructorClass> maybeResults = instructorClassService.updateClass(updateClassRequest)

        then:
        1 * classRepository.getClass(dummyClass.getId()) >> new Try.Success<InstructorClass>(dummyClass)
        1 * classRepository.saveClass(_) >> new Try.Failure<Void>(new Exception())

        then:
        maybeResults.isFailure()
    }

    def "updateClass: getClass fails, i fail"() {
        when:
        InstructorClass updateClassRequest = new InstructorClass(name: "other_class_name", instructorId: "123");
        updateClassRequest.id = dummyClass.id
        Try<InstructorClass> maybeResults = instructorClassService.updateClass(updateClassRequest)

        then:
        1 * classRepository.getClass(dummyClass.getId()) >> new Try.Failure<InstructorClass>(new Exception())

        then:
        maybeResults.isFailure()
    }

    def "updateClass: fails cannot update assessment ids"() {
        setup:
        InstructorClass updateClassRequest = new InstructorClass(name: "other_class_name", instructorId: "other_instructor_id", assessmentIds: ["3", "4"]);
        updateClassRequest.id = dummyClass.id

        when:
        Try<InstructorClass> maybeResults = instructorClassService.updateClass(updateClassRequest)

        then:
        1 * classRepository.getClass(dummyClass.getId()) >> new Try.Success<InstructorClass>(dummyClass)
        1 * userService.getInstructorById(updateClassRequest.getInstructorId()) >> new Try.Success<User>(new User())

        0 * classRepository.saveClass(_) >> { args ->
            InstructorClass instructorClass = args[0]
            assert instructorClass.id == dummyClass.id
            assert instructorClass.name == dummyClass.name

            dummyClass = instructorClass

            return new Try.Success<Void>(null)
        }

        then:
        maybeResults.isFailure()
    }

    def "updateClass: fails invalid instructor id"() {
        setup:
        InstructorClass updateClassRequest = new InstructorClass(name: "other_class_name", instructorId: "other_instructor_id", assessmentIds: ["1", "2"]);
        updateClassRequest.id = dummyClass.id

        when:
        Try<InstructorClass> maybeResults = instructorClassService.updateClass(updateClassRequest)

        then:
        1 * classRepository.getClass(dummyClass.getId()) >> new Try.Success<InstructorClass>(dummyClass)
        1 * userService.getInstructorById(updateClassRequest.getInstructorId()) >> new Try.Failure<User>(null)

        0 * classRepository.saveClass(_) >> { args ->
            InstructorClass instructorClass = args[0]
            assert instructorClass.id == dummyClass.id
            assert instructorClass.name == dummyClass.name

            dummyClass = instructorClass

            return new Try.Success<Void>(null)
        }

        then:
        maybeResults.isFailure()
    }

    def "classAssessmentTaken: success"() {
        setup:
        dummyClass.setAssessmentIds(["assessmentId"])

        when:
        Try<InstructorClass> maybeClass = instructorClassService.classAssessmentTaken("userId", "assessmentId")

        then:
        1 * classRepository.getClassByStudentAndAssessmentId(_, _) >> new Try.Success<List<InstructorClass>>([dummyClass])
        1 * classRepository.getClass(_) >> new Try.Success<InstructorClass>(dummyClass)
        1 * classRepository.saveClass(_) >> new Try.Success<InstructorClass>(dummyClass)

        then:
        maybeClass.isSuccess()
    }

    def "classAssessmentTaken: getClassesByUsername fails"() {
        setup:
        dummyClass.setAssessmentIds(["assessmentId"])

        when:
        Try<InstructorClass> maybeClass = instructorClassService.classAssessmentTaken("userId", "assessmentId")

        then:
        1 * classRepository.getClassByStudentAndAssessmentId(_, _) >> new Try.Failure<List<InstructorClass>>(null)
        0 * classRepository.getClass(_) >> new Try.Success<InstructorClass>(dummyClass)
        0 * classRepository.saveClass(_) >> new Try.Success<InstructorClass>(dummyClass)

        then:
        maybeClass.isFailure()
    }


    def "classAssessmentTaken: saveClass fails"() {
        setup:
        dummyClass.setAssessmentIds(["assessmentId"])

        when:
        Try<InstructorClass> maybeClass = instructorClassService.classAssessmentTaken("userId", "assessmentId")

        then:
        1 * classRepository.getClassByStudentAndAssessmentId(_, _) >> new Try.Success<List<InstructorClass>>([dummyClass])
        1 * classRepository.getClass(_) >> new Try.Success<InstructorClass>(dummyClass)
        1 * classRepository.saveClass(_) >> new Try.Failure<InstructorClass>(null)

        then:
        maybeClass.isFailure()
    }

    def "sendClassInvites: don't force accept success"() {
        setup:
        User user = new User()
        user.setUsername("userId")

        when:
        Try<InstructorClass> maybeClass = instructorClassService.sendClassInvites(new SendClassInviteRequest(classId: "1", userEmails: ["userId"], forceAccept: false))

        then:
        1 * classRepository.getClass('1') >> new Try.Success<InstructorClass>(dummyClass)
        1 * userService.getUser(dummyClass.getInstructorId()) >> new Try.Success<User>(user)
        1 * userService.getUserIfExists("userId") >> new Try.Success<User>(user)
        0 * pendingStudentService.inviteStudentToDaacs(_, _, _, _) >> new Try.Success<Void>(null)
        1 * classRepository.saveClass(_) >> new Try.Success<InstructorClass>(dummyClass)
        1 * mailService.sendClassInviteEmail(_, _, _) >> new Try.Success<Void>(null)

        then:
        maybeClass.isSuccess()
    }

    def "sendClassInvites: force accept success"() {
        setup:
        User user = new User()
        user.setUsername("userId")

        when:
        Try<InstructorClass> maybeClass = instructorClassService.sendClassInvites(new SendClassInviteRequest(classId: "1", userEmails: ["userId"], forceAccept: true))

        then:
        1 * classRepository.getClass('1') >> new Try.Success<InstructorClass>(dummyClass)
        1 * userService.getUser(dummyClass.getInstructorId()) >> new Try.Success<User>(user)
        1 * userService.getUserIfExists("userId") >> new Try.Success<User>(user)
        0 * pendingStudentService.inviteStudentToDaacs(_, _, _, _) >> new Try.Success<Void>(null)
        1 * classRepository.saveClass(_) >> new Try.Success<InstructorClass>(dummyClass)
        0 * mailService.sendClassInviteEmail(_, _, _) >> new Try.Success<Void>(null)

        then:
        maybeClass.isSuccess()
    }

    def "sendClassInvites: getUserByUsername fails"() {
        setup:
        User user = new User()
        user.setUsername("userId")

        when:
        Try<InstructorClass> maybeClass = instructorClassService.sendClassInvites(new SendClassInviteRequest(classId: "1", userEmails: ["userId"]))

        then:
        1 * classRepository.getClass('1') >> new Try.Success<InstructorClass>(dummyClass)
        1 * userService.getUser(dummyClass.getInstructorId()) >> new Try.Success<User>(user)
        1 * userService.getUserIfExists("userId") >> new Try.Failure<User>(null)
        0 * pendingStudentService.inviteStudentToDaacs(_, _, _, _) >> new Try.Success<Void>(null)
        0 * classRepository.getClass('1') >> new Try.Success<InstructorClass>(dummyClass)
        0 * classRepository.saveClass(_) >> new Try.Success<InstructorClass>(dummyClass)
        0 * mailService.sendClassInviteEmail(_, _, _) >> new Try.Success<Void>(null)

        then:
        maybeClass.isFailure()
    }

    def "sendClassInvites: getClass fails"() {
        setup:
        User user = new User()
        user.setUsername("userId")

        when:
        Try<InstructorClass> maybeClass = instructorClassService.sendClassInvites(new SendClassInviteRequest(classId: "1", userEmails: ["userId"]))

        then:
        1 * classRepository.getClass('1') >> new Try.Failure<InstructorClass>(null)
        0 * userService.getUser(dummyClass.getInstructorId()) >> new Try.Success<User>(user)
        0 * userService.getUserIfExists("userId") >> new Try.Success<User>(user)
        0 * pendingStudentService.inviteStudentToDaacs(_, _, _, _) >> new Try.Success<Void>(null)
        0 * classRepository.saveClass(_) >> new Try.Success<InstructorClass>(dummyClass)
        0 * mailService.sendClassInviteEmail(_, _, _) >> new Try.Success<Void>(null)

        then:
        maybeClass.isFailure()
    }

    def "sendClassInvites: sendClassInviteEmail fails"() {
        setup:
        User user = new User()
        user.setUsername("userId")

        when:
        Try<InstructorClass> maybeClass = instructorClassService.sendClassInvites(new SendClassInviteRequest(classId: "1", userEmails: ["userId"], forceAccept: false))

        then:
        1 * classRepository.getClass('1') >> new Try.Success<InstructorClass>(dummyClass)
        1 * userService.getUser(dummyClass.getInstructorId()) >> new Try.Success<User>(user)
        1 * userService.getUserIfExists("userId") >> new Try.Success<User>(user)
        0 * pendingStudentService.inviteStudentToDaacs(_, _, _, _) >> new Try.Success<Void>(null)
        1 * classRepository.saveClass(_) >> new Try.Success<InstructorClass>(dummyClass)
        1 * mailService.sendClassInviteEmail(_, _, _) >> new Try.Failure<Void>(null)

        then:
        maybeClass.isFailure()
    }

    def "sendClassInvites: user does not exist no class invite sent"() {
        setup:
        User user = new User()
        user.setUsername("userId")

        when:
        Try<InstructorClass> maybeClass = instructorClassService.sendClassInvites(new SendClassInviteRequest(classId: "1", userEmails: ["userId"], forceAccept: false))

        then:
        1 * classRepository.getClass('1') >> new Try.Success<InstructorClass>(dummyClass)
        1 * userService.getUser(dummyClass.getInstructorId()) >> new Try.Success<User>(user)
        1 * userService.getUserIfExists("userId") >> new Try.Success<User>(null)
        1 * pendingStudentService.inviteStudentToDaacs(_, _, _, _) >> new Try.Success<Void>(null)
        0 * classRepository.saveClass(_) >> new Try.Success<InstructorClass>(dummyClass)
        0 * mailService.sendClassInviteEmail(_, _, _) >> new Try.Success<Void>(null)

        then:
        maybeClass.isSuccess()
    }

    def "sendClassInvites: user does not exist, inviteStudentToDaacs fails"() {
        setup:
        User user = new User()
        user.setUsername("userId")

        when:
        Try<InstructorClass> maybeClass = instructorClassService.sendClassInvites(new SendClassInviteRequest(classId: "1", userEmails: ["userId"], forceAccept: false))

        then:
        1 * classRepository.getClass('1') >> new Try.Success<InstructorClass>(dummyClass)
        1 * userService.getUser(dummyClass.getInstructorId()) >> new Try.Success<User>(user)
        1 * userService.getUserIfExists("userId") >> new Try.Success<User>(null)
        1 * pendingStudentService.inviteStudentToDaacs(_, _, _, _) >> new Try.Failure<Void>(null)
        0 * classRepository.saveClass(_) >> new Try.Success<InstructorClass>(dummyClass)
        0 * mailService.sendClassInviteEmail(_, _, _) >> new Try.Success<Void>(null)

        then:
        maybeClass.isFailure()
    }

    def "getStudentScores: success"() {
        setup:
        User user = new User(firstName: "flamingo", lastName: "johnson", id: "userid")
        dummyClass.studentInvites.add(new StudentClassInvite("userid"))

        when:
        Try<List<InstructorClassUserScore>> maybeScores = instructorClassService.getStudentScores('1')

        then:
        1 * classRepository.getClass('1') >> new Try.Success<InstructorClass>(dummyClass)
        1 * userService.getUsersById(_) >> new Try.Success<List<User>>([user])
        2 * userAssessmentService.getLatestUserAssessmentIfExists(_, _) >> new Try.Success<UserAssessment>(dummyUserAssessments[0])

        then:
        maybeScores.isSuccess()
        maybeScores.get().size() == 1
        maybeScores.get().get(0).getStudentFirstName() == "flamingo"
        maybeScores.get().get(0).getStudentLastName() == "johnson"
        maybeScores.get().get(0).getAssessmentScores().size() == 2
        maybeScores.get().get(0).getAssessmentScores().get(0).overallScore == "MEDIUM"
    }

    def "getStudentScores: getClass fails"() {
        setup:
        User user = new User()
        dummyClass.studentInvites.add(new StudentClassInvite("userid"))

        when:
        Try<List<InstructorClassUserScore>> maybeScores = instructorClassService.getStudentScores('1')

        then:
        1 * classRepository.getClass('1') >> new Try.Failure<InstructorClass>(null)
        0 * userService.getUsersById(_) >> new Try.Success<List<User>>([user])
        0 * userAssessmentService.getLatestUserAssessmentIfExists(_, _) >> new Try.Success<UserAssessment>(dummyUserAssessments[0])

        then:
        maybeScores.isFailure()
    }

    def "getStudentScores: getUsersById fails"() {
        setup:
        User user = new User()
        dummyClass.studentInvites.add(new StudentClassInvite("userid"))

        when:
        Try<List<InstructorClassUserScore>> maybeScores = instructorClassService.getStudentScores('1')

        then:
        1 * classRepository.getClass('1') >> new Try.Success<InstructorClass>(dummyClass)
        1 * userService.getUsersById(_) >> new Try.Failure<List<User>>(null)
        0 * userAssessmentService.getLatestUserAssessmentIfExists(_, _) >> new Try.Success<UserAssessment>(dummyUserAssessments[0])

        then:
        maybeScores.isFailure()
    }

    def "getStudentScores: getUserAssessmentByUserAndAssessmentId fails"() {
        setup:
        User user = new User(firstName: "flamingo", lastName: "johnson", id: "userid")
        dummyClass.studentInvites.add(new StudentClassInvite("userid"))

        when:
        Try<List<InstructorClassUserScore>> maybeScores = instructorClassService.getStudentScores('1')

        then:
        1 * classRepository.getClass('1') >> new Try.Success<InstructorClass>(dummyClass)
        1 * userService.getUsersById(_) >> new Try.Success<List<User>>([user])
        1 * userAssessmentService.getLatestUserAssessmentIfExists(_, _) >> new Try.Failure<UserAssessment>(null)

        then:
        maybeScores.isFailure()
    }

    def "getStudentScoresCSV: success"() {
        setup:
        User user = new User(firstName: "flamingo", lastName: "johnson", id: "userid")
        dummyClass.studentInvites.add(new StudentClassInvite("userid"))

        when:
        Try<ClassScoreResults> maybeScores = instructorClassService.getStudentScoresCSV('1')

        then:
        1 * classRepository.getClass('1') >> new Try.Success<InstructorClass>(dummyClass)
        1 * userService.getUsersById(_) >> new Try.Success<List<User>>([user])
        2 * userAssessmentService.getLatestUserAssessmentIfExists(_, _) >> new Try.Success<UserAssessment>(dummyUserAssessments[0])
        2 * assessmentService.getAssessment(_) >> new Try.Success<Assessment>(dummyAssessments[0])
        0 * assessmentService.getAssessment(_)

        then:
        maybeScores.isSuccess()
        maybeScores.get().header == ["first_name", "last_name", "category_group_id", "domain", "category_group_id", "domain"]
        maybeScores.get().rows.size() == 1
        maybeScores.get().rows.get(0) == ["flamingo", "johnson", "MEDIUM", null, "MEDIUM", null]
    }

    def "getStudentScoresCSV: user hasn't taken any assessment success"() {
        setup:
        User user1 = new User(firstName: "flamingo", lastName: "johnson", id: "userid1")

        User user2 = new User(firstName: "Gorby", lastName: "Le'Bobo", id: "userid2")

        dummyClass.studentInvites.add(new StudentClassInvite("userid1"))
        dummyClass.studentInvites.add(new StudentClassInvite("userid2"))

        when:
        Try<ClassScoreResults> maybeScores = instructorClassService.getStudentScoresCSV('1')

        then:
        1 * classRepository.getClass('1') >> new Try.Success<InstructorClass>(dummyClass)
        1 * userService.getUsersById(["userid1", "userid2"]) >> new Try.Success<List<User>>([user1, user2])
        4 * userAssessmentService.getLatestUserAssessmentIfExists(_, _) >> new Try.Success<UserAssessment>(null)
        1 * assessmentService.getAssessment("1") >> new Try.Success<Assessment>(new WritingAssessment(assessmentCategoryGroup: new AssessmentCategoryGroup(id: "group_label"),
                domains: [
                        new ScoringDomain(id: "domain-1", subDomains: []),
                        new ScoringDomain(id: "domain-2", subDomains: [])
                ]))
        1 * assessmentService.getAssessment("2") >> new Try.Success<Assessment>(new WritingAssessment(assessmentCategoryGroup: new AssessmentCategoryGroup(id: "group_label2"),
                domains: [
                        new ScoringDomain(id: "domain-1b", subDomains: []),
                        new ScoringDomain(id: "domain-2b", subDomains: [])
                ]))

        then:
        maybeScores.isSuccess()
        maybeScores.get().header == ["first_name", "last_name", "group_label", "domain-1", "domain-2", "group_label2", "domain-1b", "domain-2b"]
        maybeScores.get().rows.size() == 2
        maybeScores.get().rows.get(0) == ["flamingo", "johnson", null, null, null, null, null, null]
        maybeScores.get().rows.get(1) == ["Gorby", "Le'Bobo", null, null, null, null, null, null]
    }


    def "getStudentScoresCSV: getClass fails"() {
        setup:
        User user = new User(firstName: "flamingo", lastName: "johnson", id: "userid")
        dummyClass.studentInvites.add(new StudentClassInvite("userid"))

        when:
        Try<ClassScoreResults> maybeScores = instructorClassService.getStudentScoresCSV('1')

        then:
        1 * classRepository.getClass('1') >> new Try.Failure<InstructorClass>(null)
        0 * userService.getUsersById(["userid1", "userid2"]) >> new Try.Success<List<User>>([])
        0 * userAssessmentService.getLatestUserAssessmentIfExists(_, _) >> new Try.Success<UserAssessment>(dummyUserAssessments[0])
        0 * assessmentService.getAssessment(_)

        then:
        maybeScores.isFailure()
    }

    def "getStudentScoresCSV: getUsersById fails"() {
        setup:
        User user = new User(firstName: "flamingo", lastName: "johnson", id: "userid")
        dummyClass.studentInvites.add(new StudentClassInvite("userid"))

        when:
        Try<ClassScoreResults> maybeScores = instructorClassService.getStudentScoresCSV('1')

        then:
        1 * classRepository.getClass('1') >> new Try.Success<InstructorClass>(dummyClass)
        1 * userService.getUsersById(["userid"]) >> new Try.Failure<List<User>>(null)
        0 * userAssessmentService.getLatestUserAssessmentIfExists(_, _) >> new Try.Success<UserAssessment>(dummyUserAssessments[0])
        0 * assessmentService.getAssessment(_)

        then:
        maybeScores.isFailure()
    }

    def "getStudentScoresCSV: getUserAssessmentByUserAndAssessmentId fails"() {
        setup:
        User user = new User(firstName: "flamingo", lastName: "johnson", id: "userid")
        dummyClass.studentInvites.add(new StudentClassInvite("userid"))

        when:
        Try<ClassScoreResults> maybeScores = instructorClassService.getStudentScoresCSV('1')

        then:
        1 * classRepository.getClass('1') >> new Try.Success<InstructorClass>(dummyClass)
        1 * userService.getUsersById(["userid"]) >> new Try.Success<List<User>>([user])
        1 * userAssessmentService.getLatestUserAssessmentIfExists(_, _) >> new Try.Failure<UserAssessment>(null)
        0 * assessmentService.getAssessment(_)

        then:
        maybeScores.isFailure()
    }

    def "writeScoresToCSV: success"() {
        setup:
        PipedInputStream inStream = new PipedInputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new PipedOutputStream(inStream)));

        ClassScoreResults results = new ClassScoreResults()
        results.setHeader((String[]) ["name", "assessment_name"])
        results.getRows().add((String[]) ["beepis", "LOW"])

        when:
        instructorClassService.writeScoresToCSV(new CSVWriter(writer), results)

        then:
        notThrown(Exception)

        then:
        StringBuilder stringBuilder = new StringBuilder()
        byte[] buffer = new byte[1024];
        int read;
        while ((read = inStream.read(buffer, 0, 1024)) >= 0) {
            stringBuilder.append(new String(buffer, 0, read));
        }

        stringBuilder.toString() == "\"name\",\"assessment_name\"\n" + "\"beepis\",\"LOW\"\n"
    }

    def "writeScoresToCSV: throws IOException"() {
        setup:
        CSVWriter csvWriter = Mock(CSVWriter)
        ClassScoreResults results = new ClassScoreResults(header: (String[]) [])

        when:
        instructorClassService.writeScoresToCSV(csvWriter, results)

        then:
        1 * csvWriter.writeNext(results.getHeader()) >> { throw new IOException() }

        then:
        thrown(IOException)
    }

    def "uploadClasses: creating a class success"() {
        setup:
        String fileContent =
                "Imported_Class,instructorEmail,userEmail1,assessmentsIdabc\n" +
                        "Imported_Class,instructorEmail,userEmail2,"

        MultipartFile multipartFile = new MockMultipartFile("fileName", "originalFileName", "csv", fileContent.getBytes()) as MultipartFile;

        when:
        Try<Void> maybeResponse = instructorClassService.uploadClasses(multipartFile)

        then:
        1 * userService.getUserByUsername("instructorEmail") >> new Try.Success<User>(new User(id: "instructorId" ))
        1 * userService.getUserIfExists("userEmail1") >> new Try.Success<User>(new User(id: "userid1" ))
        1 * userService.getUserIfExists("userEmail2") >> new Try.Success<User>(new User(id: "userid2" ))
        1 * classRepository.getClassByNameAndInstructor("instructorId", "Imported_Class") >> new Try.Success<InstructorClass>(null)
        1 * classRepository.insertClass(_) >> new Try.Success<Void>(null)

        then:
        maybeResponse.isSuccess()
    }

    def "uploadClasses: append students and assessments to existing class success"() {
        setup:
        dummyClass.addAssessmentId("alreadyExistingID")
        InstructorClass dummyClass2 = dummyClass
        String fileContent =
                "testClass,instructorEmail,userEmail1,assessmentsIdabc\n" +
                        "testClass,instructorEmail,userEmail2,alreadyExistingID"

        dummyClass2.addAssessmentId("assessmentsIdabc")
        dummyClass2.addStudentInvite(new StudentClassInvite(studentId: "userid1", inviteStatusAccepted: true))
        dummyClass2.addStudentInvite(new StudentClassInvite(studentId: "userid2", inviteStatusAccepted: true))
        MultipartFile multipartFile = new MockMultipartFile("fileName", "originalFileName", "csv", fileContent.getBytes()) as MultipartFile;

        when:
        Try<Void> maybeResponse = instructorClassService.uploadClasses(multipartFile)

        then:
        1 * userService.getUserByUsername("instructorEmail") >> new Try.Success<User>(new User(id: "123" ))
        1 * userService.getUserIfExists("userEmail1") >> new Try.Success<User>(new User(id: "userid1" ))
        1 * userService.getUserIfExists("userEmail2") >> new Try.Success<User>(new User(id: "userid2" ))
        1 * classRepository.getClassByNameAndInstructor("123", "testClass") >> new Try.Success<InstructorClass>(dummyClass)
        0 * classRepository.insertClass(_) >> new Try.Success<Void>(null)
        1 * classRepository.saveClass(dummyClass2) >> new Try.Success<InstructorClass>(null)

        then:
        maybeResponse.isSuccess()
    }

    def "uploadClasses: create and update classes success"() {
        setup:
        dummyClass.addAssessmentId("alreadyExistingID")
        InstructorClass dummyClass2 = dummyClass
        String fileContent =
                "Imported_Class,instructorEmail,userEmail1,assessmentsIdabc\n" +
                        "Imported_Class,instructorEmail,userEmail2,\n" +
                        "testClass,instructorEmail2,userEmail1,assessmentsIdabc\n" +
                        "testClass,instructorEmail2,userEmail2,alreadyExistingID"

        dummyClass2.addAssessmentId("assessmentsIdabc")
        dummyClass2.addStudentInvite(new StudentClassInvite(studentId: "userid1", inviteStatusAccepted: true))
        dummyClass2.addStudentInvite(new StudentClassInvite(studentId: "userid2", inviteStatusAccepted: true))
        MultipartFile multipartFile = new MockMultipartFile("fileName", "originalFileName", "csv", fileContent.getBytes()) as MultipartFile;

        when:
        Try<Void> maybeResponse = instructorClassService.uploadClasses(multipartFile)

        then:
        1 * userService.getUserByUsername("instructorEmail") >> new Try.Success<User>(new User(id: "instructorId" ))
        1 * userService.getUserIfExists("userEmail1") >> new Try.Success<User>(new User(id: "userid1" ))
        1 * userService.getUserIfExists("userEmail2") >> new Try.Success<User>(new User(id: "userid2" ))
        1 * userService.getUserByUsername("instructorEmail2") >> new Try.Success<User>(new User(id: "instructorId2" ))

        1 * classRepository.getClassByNameAndInstructor("instructorId2", "testClass") >> new Try.Success<InstructorClass>(dummyClass)
        1 * classRepository.getClassByNameAndInstructor("instructorId", "Imported_Class") >> new Try.Success<InstructorClass>(null)
        1 * classRepository.insertClass(_) >> new Try.Success<Void>(null)
        1 * classRepository.saveClass(dummyClass2) >> new Try.Success<InstructorClass>(null)

        then:
        maybeResponse.isSuccess()
    }

    def "uploadClasses: getUserByUsername fails"() {
        setup:
        String fileContent =
                "Imported_Class,instructorEmail,userEmail1,assessmentsIdabc\n" +
                        "Imported_Class,instructorEmail,userEmail2,"

        MultipartFile multipartFile = new MockMultipartFile("fileName", "originalFileName", "csv", fileContent.getBytes()) as MultipartFile;

        when:
        Try<Void> maybeResponse = instructorClassService.uploadClasses(multipartFile)

        then:
        1 * userService.getUserByUsername("instructorEmail") >> new Try.Failure<User>(null)
        0 * userService.getUserIfExists("userEmail1") >> new Try.Success<User>(new User(id: "userid1" ))
        0 * userService.getUserIfExists("userEmail2") >> new Try.Success<User>(new User(id: "userid2" ))
        0 * classRepository.getClassByNameAndInstructor("instructorId", "Imported_Class") >> new Try.Success<InstructorClass>(null)
        0 * classRepository.insertClass(_) >> new Try.Success<Void>(null)

        then:
        maybeResponse.isFailure()
    }

    def "uploadClasses: getClassByNameAndInstructor fails"() {
        setup:
        String fileContent =
                "Imported_Class,instructorEmail,userEmail1,assessmentsIdabc\n" +
                        "Imported_Class,instructorEmail,userEmail2,"

        MultipartFile multipartFile = new MockMultipartFile("fileName", "originalFileName", "csv", fileContent.getBytes()) as MultipartFile;

        when:
        Try<Void> maybeResponse = instructorClassService.uploadClasses(multipartFile)

        then:
        1 * userService.getUserByUsername("instructorEmail") >> new Try.Success<User>(new User(id: "instructorId" ))
        1 * userService.getUserIfExists("userEmail1") >> new Try.Success<User>(new User(id: "userid1" ))
        0 * userService.getUserIfExists("userEmail2") >> new Try.Success<User>(new User(id: "userid2" ))
        1 * classRepository.getClassByNameAndInstructor("instructorId", "Imported_Class") >> new Try.Failure<InstructorClass>(null)
        0 * classRepository.insertClass(_) >> new Try.Success<Void>(null)

        then:
        maybeResponse.isFailure()
    }

    def "uploadClasses:insertClass fails"() {
        setup:
        String fileContent =
                "Imported_Class,instructorEmail,userEmail1,assessmentsIdabc\n" +
                        "Imported_Class,instructorEmail,userEmail2,"

        MultipartFile multipartFile = new MockMultipartFile("fileName", "originalFileName", "csv", fileContent.getBytes()) as MultipartFile;

        when:
        Try<Void> maybeResponse = instructorClassService.uploadClasses(multipartFile)

        then:
        1 * userService.getUserByUsername("instructorEmail") >> new Try.Success<User>(new User(id: "instructorId" ))
        1 * userService.getUserIfExists("userEmail1") >> new Try.Success<User>(new User(id: "userid1" ))
        1 * userService.getUserIfExists("userEmail2") >> new Try.Success<User>(new User(id: "userid2" ))
        1 * classRepository.getClassByNameAndInstructor("instructorId", "Imported_Class") >> new Try.Success<InstructorClass>(null)
        1 * classRepository.insertClass(_) >> new Try.Failure<Void>(null)

        then:
        maybeResponse.isFailure()
    }

    def "uploadClasses: saveClass fails"() {
        setup:
        dummyClass.addAssessmentId("alreadyExistingID")
        InstructorClass dummyClass2 = dummyClass
        String fileContent =
                "testClass,instructorEmail,userEmail1,assessmentsIdabc\n" +
                        "testClass,instructorEmail,userEmail2,alreadyExistingID"

        dummyClass2.addAssessmentId("assessmentsIdabc")
        dummyClass2.addStudentInvite(new StudentClassInvite(studentId: "userid1", inviteStatusAccepted: true))
        dummyClass2.addStudentInvite(new StudentClassInvite(studentId: "userid2", inviteStatusAccepted: true))
        MultipartFile multipartFile = new MockMultipartFile("fileName", "originalFileName", "csv", fileContent.getBytes()) as MultipartFile;

        when:
        Try<Void> maybeResponse = instructorClassService.uploadClasses(multipartFile)

        then:
        1 * userService.getUserByUsername("instructorEmail") >> new Try.Success<User>(new User(id: "123" ))
        1 * userService.getUserIfExists("userEmail1") >> new Try.Success<User>(new User(id: "userid1" ))
        1 * userService.getUserIfExists("userEmail2") >> new Try.Success<User>(new User(id: "userid2" ))
        1 * classRepository.getClassByNameAndInstructor("123", "testClass") >> new Try.Success<InstructorClass>(dummyClass)
        0 * classRepository.insertClass(_) >> new Try.Success<Void>(null)
        1 * classRepository.saveClass(dummyClass2) >> new Try.Failure<InstructorClass>(null)

        then:
        maybeResponse.isFailure()
    }

    def "uploadClasses: creating a class with new student success"() {
        setup:
        String fileContent =
                "Imported_Class,instructorEmail,userEmail1,assessmentsIdabc\n" +
                        "Imported_Class,instructorEmail,userEmail2,"

        MultipartFile multipartFile = new MockMultipartFile("fileName", "originalFileName", "csv", fileContent.getBytes()) as MultipartFile;

        when:
        Try<Void> maybeResponse = instructorClassService.uploadClasses(multipartFile)

        then:
        1 * userService.getUserByUsername("instructorEmail") >> new Try.Success<User>(new User(id: "instructorId" ))
        1 * userService.getUserIfExists("userEmail1") >> new Try.Success<User>(null)
        1 * pendingStudentService.inviteStudentToDaacs("userEmail1", _, _, _) >> new Try.Success<Void>()
        1 * userService.getUserIfExists("userEmail2") >> new Try.Success<User>(new User(id: "userid2" ))
        1 * classRepository.getClassByNameAndInstructor("instructorId", "Imported_Class") >> new Try.Success<InstructorClass>(null)
        1 * classRepository.insertClass(_) >> new Try.Success<Void>(null)

        then:
        maybeResponse.isSuccess()
    }

    def "uploadClasses: pendingStudentService.inviteStudentToDaacs fails"() {
        setup:
        String fileContent =
                "Imported_Class,instructorEmail,userEmail1,assessmentsIdabc\n" +
                        "Imported_Class,instructorEmail,userEmail2,"

        MultipartFile multipartFile = new MockMultipartFile("fileName", "originalFileName", "csv", fileContent.getBytes()) as MultipartFile;

        when:
        Try<Void> maybeResponse = instructorClassService.uploadClasses(multipartFile)

        then:
        1 * userService.getUserByUsername("instructorEmail") >> new Try.Success<User>(new User(id: "instructorId" ))
        1 * userService.getUserIfExists("userEmail1") >> new Try.Success<User>(null)
        1 * classRepository.getClassByNameAndInstructor("instructorId", "Imported_Class") >> new Try.Success<InstructorClass>(null)
        1 * pendingStudentService.inviteStudentToDaacs("userEmail1", _, _, _) >> new Try.Failure<Void>()
        0 * userService.getUserIfExists("userEmail2") >> new Try.Success<User>(new User(id: "userid2" ))
        0 * classRepository.insertClass(_) >> new Try.Success<Void>(null)

        then:
        maybeResponse.isFailure()
    }
}
