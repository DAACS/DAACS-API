package com.daacs


import com.daacs.component.utils.DefaultCatgoryGroup
import com.daacs.model.InstructorClass
import com.daacs.model.PendingStudent
import com.daacs.model.User
import com.daacs.model.UserSearchResult
import com.daacs.model.assessment.*
import com.daacs.model.assessment.user.*
import com.daacs.model.item.*
import com.daacs.model.prereqs.AssessmentPrereq
import com.daacs.model.prereqs.PrereqType
import com.daacs.repository.AssessmentCategoryGroupRepository
import com.daacs.repository.AssessmentRepository
import com.daacs.repository.EventContainerRepository
import com.daacs.repository.InstructorClassRepository
import com.daacs.repository.MessageRepository
import com.daacs.repository.PendingStudentRepository
import com.daacs.repository.UserAssessmentRepository
import com.daacs.repository.UserRepository
import com.daacs.service.LightSideService
import com.daacs.service.LtiService
import com.daacs.service.S3Service
import com.google.common.collect.Range
import com.lambdista.util.Try
import org.springframework.context.annotation.*
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.authentication.encoding.ShaPasswordEncoder
import spock.lang.Ignore
import spock.lang.Specification

import javax.annotation.PostConstruct
import javax.mail.internet.MimeMessage
import javax.servlet.http.HttpServletRequest
import java.time.Instant
import java.util.function.Function
import java.util.stream.Collectors

/**
 * Created by chostetter on 7/18/16.
 */

@Profile("test")
@Configuration
@Ignore
public class TestConfiguration extends Specification {

    ShaPasswordEncoder encoder
    User dummyUser
    User dummyAdvisor
    User dummySystemUser
    User dummyAdmin
    List<Assessment> dummyAssessments
    List<UserAssessment> dummyUserAssessments
    List<Map> dummyAssessmentStats
    AssessmentCategoryGroup dummyGroup
    Map<String, UserAssessment> dummyUserAssessmentMap
    InstructorClass dummyClass

    @PostConstruct
    void init() {
        encoder = new ShaPasswordEncoder()
        dummyUser = new User(id: "9999",
                username: "testuser123",
                password: encoder.encodePassword("testpassword123", null),
                roles: ["ROLE_STUDENT", "ROLE_ADMIN"])

        dummyAdvisor = new User(id: "9999",
                username: "testadvisor123",
                password: encoder.encodePassword("testpassword123", null),
                roles: ["ROLE_ADVISOR"])

        dummyAdmin = new User(id: "9999",
                username: "testadmin123",
                password: encoder.encodePassword("testpassword123", null),
                roles: ["ROLE_ADMIN"])

        dummySystemUser = new User(id: "9999",
                username: "testsystemuser123",
                password: encoder.encodePassword("testpassword123", null),
                roles: ["ROLE_SYSTEM"])

        dummyClass = new InstructorClass(id: "123",
                name: "classname",
                instructorId: "123",
                assessmentIds: ["1"],
                canEditAssessments: true)

        dummyAssessments = [
                new CATAssessment(
                        assessmentCategory: AssessmentCategory.MATHEMATICS,
                        assessmentCategoryGroup: new AssessmentCategoryGroup(id: DefaultCatgoryGroup.MATHEMATICS_ID),
                        scoringType: ScoringType.AVERAGE,
                        id: "32c91abf-fc9b-4b41-ac4e-3f36b3e323d8",
                        minTakenGroups: 2,
                        maxTakenGroups: 3,
                        numQuestionsPerGroup: 2,
                        assessmentType: AssessmentType.CAT,
                        label: "Mathematics",
                        content: ["landing": "whatever we need to say here"],
                        enabled: true,
                        domains: [
                                new ScoringDomain(
                                        id: "domain-1",
                                        rubric: new Rubric(
                                                completionScoreMap: [
                                                        (CompletionScore.LOW)   : Range.closedOpen((Double) 0.0, (Double) 0.334),
                                                        (CompletionScore.MEDIUM): Range.closedOpen((Double) 0.334, (Double) 0.667),
                                                        (CompletionScore.HIGH)  : Range.closed((Double) 0.667, (Double) 1.0)
                                                ]
                                        )
                                ),
                                new ScoringDomain(
                                        id: "domain-2",
                                        rubric: new Rubric(
                                                completionScoreMap: [
                                                        (CompletionScore.LOW)   : Range.closedOpen((Double) 0.0, (Double) 0.334),
                                                        (CompletionScore.MEDIUM): Range.closedOpen((Double) 0.334, (Double) 0.667),
                                                        (CompletionScore.HIGH)  : Range.closed((Double) 0.667, (Double) 1.0)
                                                ]
                                        )
                                )
                        ],
                        prerequisites: [

                        ],
                        itemGroups: [
                                new CATItemGroup(id: "itemgroup-1", difficulty: Difficulty.MEDIUM, items: [
                                        new Item(id: "item-1", domainId: "domain-1", possibleItemAnswers: [new ItemAnswer(id: "answer-1", score: 1)]),
                                        new Item(id: "item-2", domainId: "domain-2", possibleItemAnswers: [new ItemAnswer(id: "answer-2", score: 1)])]),
                                new CATItemGroup(id: "itemgroup-2", difficulty: Difficulty.EASY, items: [
                                        new Item(id: "item-3", domainId: "domain-1", possibleItemAnswers: [new ItemAnswer(id: "answer-3", score: 1)]),
                                        new Item(id: "item-4", domainId: "domain-2", possibleItemAnswers: [new ItemAnswer(id: "answer-4", score: 2)])]),
                                new CATItemGroup(id: "itemgroup-3", difficulty: Difficulty.HARD, items: [
                                        new Item(id: "item-5", domainId: "domain-1", possibleItemAnswers: [new ItemAnswer(id: "answer-5", score: 3)]),
                                        new Item(id: "item-6", domainId: "domain-2", possibleItemAnswers: [new ItemAnswer(id: "answer-6", score: 2)])])
                        ],
                        overallRubric: new Rubric(
                                completionScoreMap: [
                                        (CompletionScore.LOW)   : Range.closedOpen((Double) 0.0, (Double) 0.334),
                                        (CompletionScore.MEDIUM): Range.closedOpen((Double) 0.334, (Double) 0.667),
                                        (CompletionScore.HIGH)  : Range.closed((Double) 0.667, (Double) 1.0)
                                ],
                                supplementTable: [
                                        new SupplementTableRow(completionScore: CompletionScore.HIGH, content: "abc123")
                                ]
                        )
                ),
                new MultipleChoiceAssessment(
                        assessmentCategory: AssessmentCategory.COLLEGE_SKILLS,
                        assessmentCategoryGroup: new AssessmentCategoryGroup(id: DefaultCatgoryGroup.COLLEGE_SKILLS_ID),
                        scoringType: ScoringType.SUM,
                        id: "32c91abf-fc9b-4b41-ac4e-3f36b3e323d9",
                        assessmentType: AssessmentType.LIKERT,
                        label: "College Skills",
                        content: ["landing": "whatever we need to say here"],
                        enabled: true,
                        prerequisites: [
                                new AssessmentPrereq(
                                        prereqType: PrereqType.ASSESSMENT,
                                        reason: "You must complete the writing assessment first!",
                                        assessmentCategory: AssessmentCategory.WRITING,
                                        statuses: [CompletionStatus.COMPLETED])
                        ],
                        itemGroups: [
                                new CATItemGroup(id: "itemgroup-1", difficulty: Difficulty.MEDIUM, items: [
                                        new Item(id: "item-1", possibleItemAnswers: [new ItemAnswer(id: "answer-1", score: 1)]),
                                        new Item(id: "item-2", possibleItemAnswers: [new ItemAnswer(id: "answer-2", score: 1)])]),
                                new CATItemGroup(id: "itemgroup-2", difficulty: Difficulty.EASY, items: [
                                        new Item(id: "item-3", possibleItemAnswers: [new ItemAnswer(id: "answer-3", score: 1)]),
                                        new Item(id: "item-4", possibleItemAnswers: [new ItemAnswer(id: "answer-4", score: 2)])]),
                                new CATItemGroup(id: "itemgroup-3", difficulty: Difficulty.HARD, items: [
                                        new Item(id: "item-5", possibleItemAnswers: [new ItemAnswer(id: "answer-5", score: 3)]),
                                        new Item(id: "item-6", possibleItemAnswers: [new ItemAnswer(id: "answer-6", score: 2)])])
                        ]
                ),
                new MultipleChoiceAssessment(
                        assessmentCategory: AssessmentCategory.READING,
                        assessmentCategoryGroup: new AssessmentCategoryGroup(id: DefaultCatgoryGroup.READING_ID),
                        scoringType: ScoringType.AVERAGE,
                        id: "32c91abf-fc9b-4b41-ac4e-3f36b3e323d0",
                        assessmentType: AssessmentType.MULTIPLE_CHOICE,
                        label: "Some other category",
                        content: ["landing": "whatever we need to say here"],
                        enabled: true,
                        prerequisites: []
                ),
                new WritingAssessment(
                        assessmentCategory: AssessmentCategory.WRITING,
                        assessmentCategoryGroup: new AssessmentCategoryGroup(id: DefaultCatgoryGroup.WRITING_ID),
                        scoringType: ScoringType.MANUAL,
                        id: "32c91abf-fc9b-4b41-ac4e-3f36b3e323d6",
                        assessmentType: AssessmentType.WRITING_PROMPT,
                        label: "Writing",
                        content: ["landing": "whatever we need to say here"],
                        enabled: true,
                        prerequisites: [],
                        writingPrompt: new WritingPrompt(
                                content: "write something funny",
                                minWords: 250)
                ),
                new CATAssessment(
                        assessmentCategory: AssessmentCategory.MATHEMATICS,
                        assessmentCategoryGroup: new AssessmentCategoryGroup(id: DefaultCatgoryGroup.MATHEMATICS_ID),
                        scoringType: ScoringType.AVERAGE,
                        id: "32c91abf-fc9b-4b41-ac4e-3f36b3e323d5",
                        minTakenGroups: 2,
                        maxTakenGroups: 3,
                        numQuestionsPerGroup: 2,
                        assessmentType: AssessmentType.CAT,
                        label: "Mathematics",
                        content: ["landing": "whatever we need to say here"],
                        enabled: true,
                        domains: [
                                new ScoringDomain(
                                        id: "domain-1",
                                        rubric: new Rubric(
                                                completionScoreMap: [
                                                        (CompletionScore.LOW)   : Range.closedOpen((Double) 0.0, (Double) 0.334),
                                                        (CompletionScore.MEDIUM): Range.closedOpen((Double) 0.334, (Double) 0.667),
                                                        (CompletionScore.HIGH)  : Range.closed((Double) 0.667, (Double) 1.0)
                                                ]
                                        )
                                ),
                                new ScoringDomain(
                                        id: "domain-2",
                                        rubric: new Rubric(
                                                completionScoreMap: [
                                                        (CompletionScore.LOW)   : Range.closedOpen((Double) 0.0, (Double) 0.334),
                                                        (CompletionScore.MEDIUM): Range.closedOpen((Double) 0.334, (Double) 0.667),
                                                        (CompletionScore.HIGH)  : Range.closed((Double) 0.667, (Double) 1.0)
                                                ]
                                        )
                                )
                        ],
                        prerequisites: [

                        ],
                        itemGroups: [
                                new CATItemGroup(id: "itemgroup-1", difficulty: Difficulty.MEDIUM, items: [
                                        new Item(id: "item-1", domainId: "domain-1", possibleItemAnswers: [new ItemAnswer(id: "answer-1", score: 1)]),
                                        new Item(id: "item-2", domainId: "domain-2", possibleItemAnswers: [new ItemAnswer(id: "answer-2", score: 1)])]),
                                new CATItemGroup(id: "itemgroup-2", difficulty: Difficulty.EASY, items: [
                                        new Item(id: "item-3", domainId: "domain-1", possibleItemAnswers: [new ItemAnswer(id: "answer-3", score: 1)]),
                                        new Item(id: "item-4", domainId: "domain-2", possibleItemAnswers: [new ItemAnswer(id: "answer-4", score: 2)])]),
                                new CATItemGroup(id: "itemgroup-3", difficulty: Difficulty.HARD, items: [
                                        new Item(id: "item-5", domainId: "domain-1", possibleItemAnswers: [new ItemAnswer(id: "answer-5", score: 3)]),
                                        new Item(id: "item-6", domainId: "domain-2", possibleItemAnswers: [new ItemAnswer(id: "answer-6", score: 2)])])
                        ],

                        overallRubric: new Rubric(
                                completionScoreMap: [
                                        (CompletionScore.LOW)   : Range.closedOpen((Double) 0.0, (Double) 0.334),
                                        (CompletionScore.MEDIUM): Range.closedOpen((Double) 0.334, (Double) 0.667),
                                        (CompletionScore.HIGH)  : Range.closed((Double) 0.667, (Double) 1.0)
                                ]
                        ),
                        itemGroupTransitions: [
                                new ItemGroupTransition(
                                        groupDifficulty: Difficulty.EASY,
                                        transitionMap: [
                                                (Difficulty.EASY)  : Range.atMost((Double) 2),
                                                (Difficulty.MEDIUM): Range.atLeast((Double) 3)
                                        ]
                                ),
                                new ItemGroupTransition(
                                        groupDifficulty: Difficulty.MEDIUM,
                                        transitionMap: [
                                                (Difficulty.EASY)  : Range.atMost((Double) 2),
                                                (Difficulty.MEDIUM): Range.closed((Double) 3, (Double) 4),
                                                (Difficulty.HARD)  : Range.atLeast((Double) 5)
                                        ]
                                ),
                                new ItemGroupTransition(
                                        groupDifficulty: Difficulty.HARD,
                                        transitionMap: [
                                                (Difficulty.MEDIUM): Range.atMost((Double) 4),
                                                (Difficulty.HARD)  : Range.atLeast((Double) 5)
                                        ]
                                )
                        ]
                ),
                new WritingAssessment(
                        scoringType: ScoringType.MANUAL,
                        id: "writing-assessment-1",
                        assessmentType: AssessmentType.WRITING_PROMPT,
                        domains: [
                                new ScoringDomain(
                                        id: "domain-1",
                                        label: "domain-1",
                                        content: "",
                                        rubric: new Rubric(
                                                completionScoreMap: [:],
                                                supplementTable: [
                                                        new SupplementTableRow(completionScore: CompletionScore.HIGH)
                                                ]))
                        ],
                        prerequisites: [],
                        overallRubric: new Rubric(
                                completionScoreMap: [:],
                                supplementTable: [
                                        new SupplementTableRow(completionScore: CompletionScore.HIGH)
                                ]),
                        assessmentCategory: AssessmentCategory.WRITING,
                        assessmentCategoryGroup: new AssessmentCategoryGroup(id: "AnotherWRITING-Group"),
                        enabled: true,
                        label: "Writing",
                        writingPrompt: new WritingPrompt(content: "", minWords: 1)
                ),
                new WritingAssessment(
                        scoringType: ScoringType.MANUAL,
                        id: "writing-assessment-2",
                        assessmentType: AssessmentType.WRITING_PROMPT,
                        domains: [
                                new ScoringDomain(
                                        id: "domain-1",
                                        label: "domain-1",
                                        content: "",
                                        rubric: new Rubric(
                                                completionScoreMap: [:],
                                                supplementTable: [
                                                        new SupplementTableRow(completionScore: CompletionScore.HIGH)
                                                ]))
                        ],
                        prerequisites: [],
                        overallRubric: new Rubric(
                                completionScoreMap: [:],
                                supplementTable: [
                                        new SupplementTableRow(completionScore: CompletionScore.HIGH)
                                ]),
                        assessmentCategory: AssessmentCategory.WRITING,
                        assessmentCategoryGroup: new AssessmentCategoryGroup(id: "AnotherWRITING-Group"),
                        enabled: true,
                        label: "Writing",
                        writingPrompt: new WritingPrompt(content: "", minWords: 1)
                )
        ]

        dummyAssessmentStats = [
                ["assessmentId"      : "eeba6280-a367-431a-ae04-e0dc0882aa29",
                 "status"            : "IN_PROGRESS",
                 "assessmentCategory": "WRITING",
                 "total"             : "1"],
                ["assessmentId"      : "eeba6280-a367-431a-ae04-e0dc0882aa29",
                 "status"            : "COMPLETED",
                 "assessmentCategory": "WRITING",
                 "total"             : "2"],
                ["assessmentId"      : "eeba6280-a367-431a-ae04-e0dc0882aa29",
                 "status"            : "GRADED",
                 "assessmentCategory": "WRITING",
                 "total"             : "3"],
                ["assessmentId"      : "aaba6280-a367-431a-ae04-e0dc0882aa29",
                 "status"            : "IN_PROGRESS",
                 "assessmentCategory": "READING",
                 "total"             : "4"]
        ]

        dummyUserAssessments = [

                new CATUserAssessment(
                        scoringType: ScoringType.AVERAGE,
                        assessmentId: dummyAssessments.get(0).getId(),
                        assessmentCategory: AssessmentCategory.MATHEMATICS,
                        assessmentCategoryGroupId: dummyAssessments.get(0).getAssessmentCategoryGroup().getId(),
                        firstName: "dummy",
                        lastName: "user",
                        userId: "user-123",
                        username: "user123",
                        assessmentType: AssessmentType.CAT,
                        completionDate: Instant.now(),
                        overallScore: CompletionScore.MEDIUM,
                        progressPercentage: 1.0,
                        status: CompletionStatus.GRADED,
                        takenDate: Instant.parse("2016-01-01T00:00:00.000Z"),
                        domainScores: [new DomainScore(domainId: "domain-1", rubricScore: CompletionScore.MEDIUM), new DomainScore(domainId: "domain-2", rubricScore: CompletionScore.HIGH)],
                        itemGroups: [
                                new CATItemGroup(difficulty: Difficulty.EASY, items: [
                                        new Item(
                                                question: "abc?",
                                                domainId: "domain-1",
                                                possibleItemAnswers: [new ItemAnswer(content: "abc", score: 1.0)],
                                                chosenItemAnswerId: null,
                                                startDate: Instant.now(),
                                                completeDate: Instant.now()
                                        )
                                ]),
                                new CATItemGroup(difficulty: Difficulty.MEDIUM, items: [
                                        new Item(
                                                question: "def?",
                                                domainId: "domain-2",
                                                possibleItemAnswers: [new ItemAnswer(content: "def", score: 1.0)],
                                                chosenItemAnswerId: null,
                                                startDate: Instant.now(),
                                                completeDate: Instant.now()
                                        )
                                ])]
                ),
                new MultipleChoiceUserAssessment(
                        scoringType: ScoringType.AVERAGE,
                        assessmentId: dummyAssessments.get(1).getId(),
                        assessmentCategory: AssessmentCategory.READING,
                        assessmentCategoryGroupId: dummyAssessments.get(1).getAssessmentCategoryGroup().getId(),
                        status: CompletionStatus.IN_PROGRESS,
                        takenDate: Instant.parse("2015-01-01T00:00:00.000Z"),
                        itemGroups: [
                                new CATItemGroup(difficulty: Difficulty.EASY, items: [
                                        new Item(
                                                question: "abc?",
                                                domainId: "domain",
                                                possibleItemAnswers: [new ItemAnswer(content: "abc", score: 1.0)],
                                                chosenItemAnswerId: null,
                                                startDate: Instant.now(),
                                                completeDate: Instant.now()
                                        )
                                ]),
                                new CATItemGroup(difficulty: Difficulty.MEDIUM, items: [
                                        new Item(
                                                question: "def?",
                                                domainId: "domain",
                                                possibleItemAnswers: [new ItemAnswer(content: "def", score: 1.0)],
                                                chosenItemAnswerId: null,
                                                startDate: Instant.now(),
                                                completeDate: Instant.now()
                                        )
                                ])]
                ),
                new WritingPromptUserAssessment(
                        scoringType: ScoringType.MANUAL,
                        assessmentCategory: AssessmentCategory.WRITING,
                        assessmentCategoryGroupId: dummyAssessments.get(3).getAssessmentCategoryGroup().getId(),
                        id: "user-assessment-1",
                        userId: "user123",
                        assessmentId: dummyAssessments.get(3).getId(),
                        assessmentType: AssessmentType.WRITING_PROMPT,
                        completionDate: Instant.now(),
                        overallScore: CompletionScore.MEDIUM,
                        progressPercentage: 1.0,
                        status: CompletionStatus.IN_PROGRESS,
                        takenDate: Instant.parse("2014-01-01T00:00:00.000Z"),
                        domainScores: [new DomainScore(domainId: "domain", rubricScore: CompletionScore.MEDIUM)],
                        writingPrompt: new WritingPrompt(
                                content: "",
                                minWords: 250,
                                sample: "this is my writing sample",
                                startDate: Instant.parse("2014-01-01T00:00:00.000Z"),
                                completeDate: Instant.parse("2014-01-02T00:00:00.000Z"))
                ),
                new MultipleChoiceUserAssessment(
                        assessmentCategory: AssessmentCategory.READING,
                        assessmentCategoryGroupId: dummyAssessments.get(0).getAssessmentCategoryGroup().getId(),
                        scoringType: ScoringType.MANUAL,
                        assessmentId: dummyAssessments.get(0).getId(),
                        takenDate: Instant.parse("2013-01-01T00:00:00.000Z")),
                new MultipleChoiceUserAssessment(
                        id: "c7e98100-935e-4f73-b1ae-f4c09c8106a4",
                        assessmentCategory: AssessmentCategory.READING,
                        assessmentCategoryGroupId: dummyAssessments.get(0).getAssessmentCategoryGroup().getId(),
                        scoringType: ScoringType.MANUAL,
                        status: CompletionStatus.COMPLETED,
                        assessmentId: dummyAssessments.get(0).getId(),
                        takenDate: Instant.parse("2013-01-01T00:00:00.000Z")),
                new CATUserAssessment(
                        assessmentCategory: AssessmentCategory.MATHEMATICS,
                        assessmentCategoryGroupId: dummyAssessments.get(1).getAssessmentCategoryGroup().getId(),
                        scoringType: ScoringType.AVERAGE,
                        assessmentId: dummyAssessments.get(1).getId(),
                        takenDate: Instant.parse("2012-01-01T00:00:00.000Z")),
                new WritingPromptUserAssessment(
                        assessmentCategory: AssessmentCategory.WRITING,
                        assessmentCategoryGroupId: dummyAssessments.get(2).getAssessmentCategoryGroup().getId(),
                        scoringType: ScoringType.MANUAL,
                        assessmentId: dummyAssessments.get(2).getId(),
                        takenDate: Instant.parse("2011-01-01T00:00:00.000Z")),
                new CATUserAssessment(
                        assessmentCategory: AssessmentCategory.MATHEMATICS,
                        assessmentCategoryGroupId: dummyAssessments.get(0).getAssessmentCategoryGroup().getId(),
                        scoringType: ScoringType.AVERAGE,
                        assessmentId: dummyAssessments.get(0).getId(),
                        status: CompletionStatus.COMPLETED,
                        takenDate: Instant.parse("2010-01-01T00:00:00.000Z")),
                new WritingPromptUserAssessment(
                        assessmentCategory: AssessmentCategory.WRITING,
                        assessmentCategoryGroupId: dummyAssessments.get(1).getAssessmentCategoryGroup().getId(),
                        scoringType: ScoringType.MANUAL,
                        assessmentId: dummyAssessments.get(1).getId(),
                        status: CompletionStatus.COMPLETED,
                        takenDate: Instant.parse("2009-01-01T00:00:00.000Z")),
                new MultipleChoiceUserAssessment(
                        assessmentCategory: AssessmentCategory.COLLEGE_SKILLS,
                        assessmentCategoryGroupId: dummyAssessments.get(2).getAssessmentCategoryGroup().getId(),
                        scoringType: ScoringType.AVERAGE,
                        assessmentId: dummyAssessments.get(2).getId(),
                        status: CompletionStatus.COMPLETED,
                        takenDate: Instant.parse("2008-01-01T00:00:00.000Z")),
                new CATUserAssessment(
                        assessmentCategory: AssessmentCategory.MATHEMATICS,
                        assessmentCategoryGroupId: dummyAssessments.get(0).getAssessmentCategoryGroup().getId(),
                        scoringType: ScoringType.AVERAGE,
                        id: "42c91abf-fc9b-4b41-ac4e-3f36b3e323d8",
                        assessmentId: dummyAssessments.get(0).getId(),
                        assessmentType: AssessmentType.CAT,
                        completionDate: Instant.now(),
                        overallScore: CompletionScore.MEDIUM,
                        progressPercentage: 1.0,
                        status: CompletionStatus.GRADED,
                        takenDate: Instant.parse("2016-01-01T00:00:00.000Z"),
                        domainScores: [new DomainScore(domainId: "domain", rubricScore: CompletionScore.MEDIUM)],
                        itemGroups: [
                                new CATItemGroup(difficulty: Difficulty.EASY, items: [
                                        new Item(
                                                question: "abc?",
                                                domainId: "domain-1",
                                                possibleItemAnswers: [new ItemAnswer(id: "answer-1", content: "abc", score: 1.0)],
                                                chosenItemAnswerId: "answer-1",
                                                startDate: Instant.now(),
                                                completeDate: Instant.now()
                                        )
                                ]),
                                new CATItemGroup(difficulty: Difficulty.MEDIUM, items: [
                                        new Item(
                                                question: "def?",
                                                domainId: "domain-2",
                                                possibleItemAnswers: [new ItemAnswer(id: "answer-1", content: "def", score: 1.0)],
                                                chosenItemAnswerId: "answer-1",
                                                startDate: Instant.now(),
                                                completeDate: Instant.now()
                                        )
                                ])]
                ),
                new CATUserAssessment(
                        assessmentCategory: AssessmentCategory.MATHEMATICS,
                        assessmentCategoryGroupId: dummyAssessments.get(4).getAssessmentCategoryGroup().getId(),
                        scoringType: ScoringType.AVERAGE,
                        id: "42c91abf-fc9b-4b41-ac4e-3f36b3e323d4",
                        assessmentId: dummyAssessments.get(4).getId(),
                        assessmentType: AssessmentType.CAT,
                        overallScore: CompletionScore.MEDIUM,
                        progressPercentage: 0.5,
                        status: CompletionStatus.IN_PROGRESS,
                        takenDate: Instant.parse("2016-01-01T00:00:00.000Z"),
                        domainScores: [new DomainScore(domainId: "domain", rubricScore: CompletionScore.MEDIUM)],
                        itemGroups: [
                                new CATItemGroup(difficulty: Difficulty.EASY, items: [
                                        new Item(
                                                question: "abc?",
                                                domainId: "domain-1",
                                                possibleItemAnswers: [new ItemAnswer(id: "answer-1", content: "abc", score: 1.0)],
                                                chosenItemAnswerId: "answer-1",
                                                startDate: Instant.now(),
                                                completeDate: Instant.now()
                                        )
                                ]),
                                new CATItemGroup(difficulty: Difficulty.MEDIUM, items: [
                                        new Item(
                                                question: "def?",
                                                domainId: "domain-2",
                                                possibleItemAnswers: [new ItemAnswer(id: "answer-1", content: "def", score: 1.0)],
                                                chosenItemAnswerId: "answer-1",
                                                startDate: Instant.now(),
                                                completeDate: Instant.now()
                                        )
                                ])]
                )
        ];

        dummyGroup = new AssessmentCategoryGroup(
                id: "id",
                label: "lebleele",
                assessmentCategory: "MATHEMATICS"
        )
    }

    @Bean
    @Primary
    @Scope("singleton")
    public UserRepository userRepository() {
        UserRepository userRepository = Mock(UserRepository)

        userRepository.getUser("9999") >> new Try.Success<User>(dummyUser)
        userRepository.getUser(_) >> new Try.Success<User>(dummyUser)
        userRepository.getUserByUsername("testuser123") >> new Try.Success<User>(dummyUser)
        userRepository.getUserByUsername("testuser123@test.com") >> new Try.Success<User>(dummyUser)
        userRepository.getUserByUsername("testadvisor123") >> new Try.Success<User>(dummyAdvisor)
        userRepository.getUserByUsername("testadmin123") >> new Try.Success<User>(dummyAdmin)
        userRepository.getUserByUsername("testsystemuser123") >> new Try.Success<User>(dummySystemUser)
        userRepository.insertUser(_) >> new Try.Success<Void>(null)
        userRepository.getUsers(_) >> new Try.Success<List<User>>([dummyUser])


        userRepository.saveUser(_) >> { args ->
            User user = args[0]
            if (user.resetPasswordCode != null) {
                user.setResetPasswordCode("123456")
            }

            return new Try.Success<User>(user)
        }

        userRepository.searchUsers(["dummy", "user"], null, _) >> new Try.Success<List<UserSearchResult>>([new UserSearchResult(firstName: "dummy", lastName: "user", username: "dummyuser")]);


        return userRepository;
    }

    @Bean
    @Primary
    @Scope("singleton")
    public EventContainerRepository eventContainerRepository() {
        EventContainerRepository eventContainerRepository = Mock(EventContainerRepository)

        eventContainerRepository.recordUserEvent(_, _) >> new Try.Success<Void>(null)

        return eventContainerRepository;
    }

    @Bean
    @Primary
    @Scope("singleton")
    public MessageRepository messageRepository() {
        MessageRepository messageRepository = Mock(MessageRepository)

        messageRepository.insertMessage(_) >> new Try.Success<Void>(null)

        return messageRepository;
    }

    @Bean
    @Primary
    @Scope("singleton")
    public AssessmentRepository assessmentRepository() {
        AssessmentRepository assessmentRepository = Mock(AssessmentRepository)

        //getAssessments(Boolean enabled, List<String> groupIds);
        assessmentRepository.getAssessments(true, null) >> new Try.Success<List<Assessment>>(dummyAssessments)
        assessmentRepository.getAssessments(true, _) >> new Try.Success<List<Assessment>>(dummyAssessments)
        assessmentRepository.getAssessments(false, _) >> new Try.Success<List<Assessment>>([])

        //getAssessments(List<ScoringType> scoringTypes, Boolean enabled);
        assessmentRepository.getAssessments(null, _) >> new Try.Success<List<Assessment>>(dummyAssessments)
        assessmentRepository.getAssessments(_, true) >> new Try.Success<List<Assessment>>([])

        assessmentRepository.getAssessment(_ as String) >> { args ->
            String id = args[0]
            return new Try.Success<Assessment>(dummyAssessments.find { it.id == id });
        }

        assessmentRepository.getAssessments(_, true, null) >> { args ->
            List<ScoringType> scoringTypes = args[0]
            return new Try.Success<List<Assessment>>(dummyAssessments.findAll {
                scoringTypes.contains(it.scoringType)
            });
        }

        assessmentRepository.insertAssessment(_) >> { args ->
            return new Try.Success<Assessment>(args[0]);
        }

        assessmentRepository.saveAssessment(_) >> { args ->
            return new Try.Success<Assessment>(args[0]);
        }

        assessmentRepository.getAssessmentStats() >> { args ->
            return new Try.Success<List<Map>>(dummyAssessmentStats);
        }

        return assessmentRepository;
    }

    @Bean
    @Primary
    @Scope("singleton")
    public JavaMailSender javaMailSender() {
        JavaMailSender javaMailSender = Mock(JavaMailSender)
        javaMailSender.createMimeMessage() >> Mock(MimeMessage)
        return javaMailSender;
    }

    @Bean
    @Primary
    @Scope("singleton")
    public LightSideService lightSideService() {
        LightSideService lightSideService = Mock(LightSideService)
        lightSideService.setupFileSystem() >> new Try.Success<Void>(null)
        lightSideService.saveUploadedModelFile(_) >> new Try.Success<Void>(null)
        return lightSideService;
    }

    @Bean
    @Primary
    @Scope("singleton")
    public UserAssessmentRepository userAssessmentRepository() {
        UserAssessmentRepository userAssessmentRepository = Mock(UserAssessmentRepository)

        userAssessmentRepository.getUserAssessmentsByAssessmentId(_ as String) >> { arguments ->
            String assessmentId = arguments[0]
            return new Try.Success<List<UserAssessment>>(dummyUserAssessments.findAll{ assessmentId.contains(it.id) });
        }

        userAssessmentRepository.getLatestUserAssessments(
                dummyUser.id,
                dummyAssessments.collect { it.getId() }) >> new Try.Success<Map<String, UserAssessment>>(
                [dummyUserAssessments.get(1)].
                        stream().
                        collect(Collectors.toMap(new Function<UserAssessment, String>() {
                            public String apply(UserAssessment p) { return p.getAssessmentId(); }
                        },
                                Function.<UserAssessment> identity())))
                /**
                 * Note for above ^^:
                 * 'collect(Collectors.toMap(UserAssessment::getAssessmentCategoryGroupId(),it -> it ))'
                 * doesn't work in test directory
                 **/


        userAssessmentRepository.getLatestUserAssessments(
                "8888",
                dummyAssessments.collect {
                    it.getId()
                }) >> new Try.Success<Map<String, UserAssessment>> ([dummyUserAssessments.get(0)].stream().
                collect(Collectors.toMap(new Function<UserAssessment, String>() {
                    String apply(UserAssessment p) { return p.getAssessmentId() }
                },
                        Function.<UserAssessment> identity())))

        userAssessmentRepository.getLatestUserAssessments(_, []) >> new Try.Success<Map<String, UserAssessment>>(new HashMap<String, UserAssessment>())

        userAssessmentRepository.getUserAssessment(
                dummyUser.id,
                dummyUserAssessments.get(0).assessmentId,
                dummyUserAssessments.get(0).takenDate) >> new Try.Success<UserAssessment>(dummyUserAssessments.get(0))

        userAssessmentRepository.getUserAssessmentById(_, _ as String) >> { args ->
            String id = args[1]
            new Try.Success<UserAssessment>(dummyUserAssessments.find { it.id == id })
        }

        userAssessmentRepository.getUserAssessment(
                dummyUser.id,
                dummyUserAssessments.get(2).assessmentId,
                dummyUserAssessments.get(2).takenDate) >> new Try.Success<UserAssessment>(dummyUserAssessments.get(2))

        userAssessmentRepository.getLatestUserAssessment(dummyUser.id, _ as String) >> { arguments ->
            String assessmentId = arguments[1]
            UserAssessment userAssessment = dummyUserAssessments.find { it.assessmentId == assessmentId }
            return new Try.Success<UserAssessment>(userAssessment)
        }

        userAssessmentRepository.getLatestUserAssessmentByGroup(dummyUser.id, _ ) >> { arguments ->
            String groupId = arguments[1]
            UserAssessment userAssessment = dummyUserAssessments.find {
                it.assessmentCategoryGroupId == groupId
            }
            return new Try.Success<UserAssessment>(userAssessment)
        }

        userAssessmentRepository.insertUserAssessment(_) >> { arguments ->
            UserAssessment userAssessment = arguments[0]
            return new Try.Success<UserAssessment>(userAssessment)
        }

        userAssessmentRepository.getUserAssessments(
                dummyUser.id,
                dummyUserAssessments.get(0).assessmentId) >> new Try.Success<List<UserAssessment>>([dummyUserAssessments.get(0)])

        userAssessmentRepository.getUserAssessmentById(
                dummyUser.id,
                dummyUserAssessments.get(9).id) >> new Try.Success<UserAssessment>(dummyUserAssessments.get(9))

        userAssessmentRepository.getUserAssessments(
                dummyUser.id,
                dummyUserAssessments.get(0).assessmentId,
                dummyUserAssessments.get(0).takenDate) >> new Try.Success<List<UserAssessment>>([dummyUserAssessments.get(0)])


        userAssessmentRepository.getUserAssessments(_, _, null) >> { args ->
            String groupId = args[1]
            return new Try.Success<List<UserAssessment>>(dummyUserAssessments.findAll {
                groupId.contains(it.assessmentCategoryGroupId)
            });
        }

        userAssessmentRepository.getUserAssessmentsByGroupId(_, _, null) >> { args ->
            String groupId = args[1]
            return new Try.Success<List<UserAssessment>>(dummyUserAssessments.findAll {
                groupId.contains(it.assessmentCategoryGroupId)
            });
        }

        userAssessmentRepository.getUserAssessments(_) >> new Try.Success<List<UserAssessment>>(dummyUserAssessments)

        userAssessmentRepository.saveUserAssessment(_) >> { arguments ->
            return new Try.Success<UserAssessment>(arguments[0])
        }

        userAssessmentRepository.getLatestUserAssessment("user-123") >> new Try.Success<UserAssessment>(dummyUserAssessments.get(0))

        userAssessmentRepository.getUserAssessments(_ as List<CompletionStatus>, _ as List<String>) >> { args ->
            List<CompletionStatus> statuses = args[0]
            return new Try.Success<List<UserAssessment>>(dummyUserAssessments.findAll {
                statuses.contains(it.status)
            });
        }

        userAssessmentRepository.getCompletedUserAssessments(_, _) >> new Try.Success<List<UserAssessment>>(dummyUserAssessments);

        userAssessmentRepository.getUserAssessments(_ as List<CompletionStatus>, null, _, _, _) >> { args ->
            List<CompletionStatus> statuses = args[0]
            return new Try.Success<List<UserAssessment>>(dummyUserAssessments.findAll {
                statuses.contains(it.status)
            });
        }

        userAssessmentRepository.getUserAssessments(_ as List<CompletionStatus>, _ as List<ScoringType>, _, _, _) >> { args ->
            List<CompletionStatus> statuses = args[0]
            List<ScoringType> scoringTypes  = args[1]
            return new Try.Success<List<UserAssessment>>(dummyUserAssessments.findAll {
                statuses.contains(it.status)and(scoringTypes.contains(it.scoringType))
            })
        }

        userAssessmentRepository.getUserAssessmentsByCategory(_,_) >> {
            return new Try.Success<Map<String, List<UserAssessment>>>(dummyUserAssessments.collectEntries {
                [(it.getAssessmentCategoryGroupId()): [it]]
            })
        }

        userAssessmentRepository.getUserAssessments(_, CompletionStatus.COMPLETED, _, _) >> { args ->
            return new Try.Success<List<UserAssessment>>(dummyUserAssessments.stream().filter { ua -> ua.getStatus() == CompletionStatus.COMPLETED }.collect(Collectors.toList()));
        }

        userAssessmentRepository.getUserAssessments(_, CompletionStatus.GRADED, _, _) >> { args ->
            return new Try.Success<List<UserAssessment>>(dummyUserAssessments.stream().filter { ua -> ua.getStatus() == CompletionStatus.GRADED }.collect(Collectors.toList()));
        }

        userAssessmentRepository.getUserAssessments(_, CompletionStatus.IN_PROGRESS, _, _) >> { args ->
            return new Try.Success<List<UserAssessment>>(dummyUserAssessments.stream().filter { ua -> ua.getStatus() == CompletionStatus.IN_PROGRESS }.collect(Collectors.toList()));
        }

        userAssessmentRepository.getUserAssessments(_, CompletionStatus.GRADING_FAILURE, _, _) >> { args ->
            return new Try.Success<List<UserAssessment>>(dummyUserAssessments.stream().filter { ua -> ua.getStatus() == CompletionStatus.GRADING_FAILURE }.collect(Collectors.toList()));
        }

        return userAssessmentRepository;
    }

    @Bean
    @Primary
    @Scope("singleton")
    public S3Service s3Service() {
        S3Service s3Service = Mock(S3Service)
        s3Service.storeImage(_, _) >> new Try.Success<URL>(new URL("https://bucket.s3.amazonaws.com/bucketName/200.file"))
        return s3Service
    }

    @Bean
    @Primary
    @Scope("singleton")
    public AssessmentCategoryGroupRepository groupRepository() {
        AssessmentCategoryGroupRepository groupRepository = Mock(AssessmentCategoryGroupRepository)

        groupRepository.getCategoryGroups() >> new Try.Success<List<AssessmentCategoryGroup>>([dummyGroup])
        groupRepository.getCategoryGroupById(_) >> new Try.Success<AssessmentCategoryGroup>(dummyGroup)
        groupRepository.createCategoryGroup(_) >> new Try.Success<AssessmentCategoryGroup>(dummyGroup)
        groupRepository.updateCategoryGroup(_) >> new Try.Success<Void>(null)
        groupRepository.deleteCategoryGroup(_) >> new Try.Success<Void>(null)
        groupRepository.getGlobalGroups() >> new Try.Success<Try<List<AssessmentCategoryGroup>>> ([])

        groupRepository.getCategoryGroupByIdOrNull("id") >> new Try.Success<AssessmentCategoryGroup>(null )
        groupRepository.getCategoryGroupByIdOrNull(_) >> new Try.Success<AssessmentCategoryGroup>(dummyGroup )


        return groupRepository
    }

    @Bean
    @Primary
    @Scope("singleton")
    public LtiService ltiLaunchService() {
        LtiService ltiLaunchService = Mock(LtiService)
        ltiLaunchService.verifyLaunch(_ as HttpServletRequest) >> new Try.Success<String>("token")
        return ltiLaunchService
    }

    @Bean
    @Primary
    @Scope("singleton")
    public InstructorClassRepository instructorClassRepository() {
        InstructorClassRepository instructorClassRepository = Mock(InstructorClassRepository)

        instructorClassRepository.getClasses() >> new Try.Success<List<InstructorClass>>([dummyClass])
        instructorClassRepository.getClassByStudentAndAssessmentId(_ as String, _ as String) >> new Try.Success<List<InstructorClass>>([dummyClass])
        instructorClassRepository.getClassByStudentAndAssessmentId(null, null) >> new Try.Success<List<InstructorClass>>([dummyClass])
        instructorClassRepository.getClass(_ as String) >> new Try.Success<InstructorClass>(dummyClass)
        instructorClassRepository.saveClass(_ as InstructorClass) >> new Try.Success<InstructorClass>(dummyClass)
        instructorClassRepository.insertClass(_ as InstructorClass) >> new Try.Success<Void>(null)

        return instructorClassRepository
    }

    @Bean
    @Primary
    @Scope("singleton")
    public PendingStudentRepository pendingStudentRepository() {
        PendingStudentRepository pendingStudentRepository = Mock(PendingStudentRepository)

        pendingStudentRepository.getPendStudent(_ as String) >> new Try.Success<PendingStudent>(null)
        pendingStudentRepository.insertPendStudent(_)  >> new Try.Success<Void>(null)
        pendingStudentRepository.deletePendStudent(_ as String) >> new Try.Success<Void>(null)

        return pendingStudentRepository
    }

}
