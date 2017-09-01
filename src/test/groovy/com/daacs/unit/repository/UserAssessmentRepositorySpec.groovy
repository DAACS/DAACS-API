package com.daacs.repository

import com.daacs.component.HystrixCommandFactory
import com.daacs.framework.exception.AlreadyExistsException
import com.daacs.framework.exception.RepoNotFoundException
import com.daacs.framework.hystrix.FailureType
import com.daacs.framework.hystrix.FailureTypeException
import com.daacs.model.assessment.AssessmentCategory
import com.daacs.model.assessment.ScoringType
import com.daacs.model.assessment.user.*
import com.daacs.repository.hystrix.*
import com.lambdista.util.Try
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant
/**
 * Created by chostetter on 6/22/16.
 */
class UserAssessmentRepositorySpec extends Specification {

    UserAssessmentRepository userAssessmentRepository

    HystrixCommandFactory hystrixCommandFactory
    MongoInsertCommand mongoInsertCommand
    MongoSaveCommand mongoSaveCommand
    MongoFindCommand mongoFindCommand
    MongoFindOneCommand mongoFindOneCommand
    MongoFindByIdCommand mongoFindByIdCommand

    FailureTypeException ioFailureTypeException = new FailureTypeException("failure", "failure", FailureType.RETRYABLE, new IOException());

    String userId = UUID.randomUUID().toString()
    String assessmentId = UUID.randomUUID().toString()
    Instant takenDate = Instant.now()
    String userAssessmentId = UUID.randomUUID().toString()

    def setup(){

        mongoInsertCommand = Mock(MongoInsertCommand)
        mongoSaveCommand = Mock(MongoSaveCommand)
        mongoFindCommand = Mock(MongoFindCommand)
        mongoFindOneCommand = Mock(MongoFindOneCommand)
        mongoFindByIdCommand = Mock(MongoFindByIdCommand)

        hystrixCommandFactory = Mock(HystrixCommandFactory)
        hystrixCommandFactory.getMongoInsertCommand(*_) >> mongoInsertCommand
        hystrixCommandFactory.getMongoSaveCommand(*_) >> mongoSaveCommand
        hystrixCommandFactory.getMongoFindCommand(*_) >> mongoFindCommand
        hystrixCommandFactory.getMongoFindOneCommand(*_) >> mongoFindOneCommand
        hystrixCommandFactory.getMongoFindByIdCommand(*_) >> mongoFindByIdCommand

        userAssessmentRepository = new UserAssessmentRepositoryImpl(hystrixCommandFactory: hystrixCommandFactory)
    }

    def "getUserAssessment: success"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getUserAssessment(userId, assessmentId, takenDate)

        then:
        1 * hystrixCommandFactory.getMongoFindOneCommand(*_) >> { arguments ->
            Query query = arguments[2]

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "userId" && criteria.isValue == userId
            } != null

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "assessmentId" && criteria.isValue == assessmentId
            } != null

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "takenDate" && criteria.isValue == Date.from(takenDate)
            } != null

            return mongoFindOneCommand
        }

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<UserAssessment>(Mock(UserAssessment))
        maybeUserAssessment.isSuccess()
    }

    def "getUserAssessment: failed, return failure"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getUserAssessment(userId, assessmentId, takenDate)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Failure<UserAssessment>(new RepoNotFoundException("not found"))
        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof RepoNotFoundException
    }

    def "getUserAssessment: success w/null, return failure"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getUserAssessment(userId, assessmentId, takenDate)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<UserAssessment>(null)
        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof RepoNotFoundException
    }

    def "getUserAssessments w/takenDate: single assessmentId success"(){
        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessments(userId, assessmentId, takenDate)

        then:
        1 * hystrixCommandFactory.getMongoFindCommand(*_) >> { arguments ->
            Query query = arguments[2]

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "userId" && criteria.isValue == userId
            } != null

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "assessmentId" && criteria.isValue == assessmentId
            } != null

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "takenDate" && criteria.isValue == Date.from(takenDate)
            } != null

            return mongoFindCommand
        }

        then:
        1 * mongoFindCommand.execute() >> new Try.Success<List<UserAssessment>>([Mock(UserAssessment)])
        maybeUserAssessments.isSuccess()
    }

    def "getUserAssessments by category: success"(){
        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessments(userId, AssessmentCategory.MATHEMATICS, null)

        then:
        1 * hystrixCommandFactory.getMongoFindCommand(*_) >> { arguments ->
            Query query = arguments[2]

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "userId" && criteria.isValue == userId
            } != null

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "assessmentCategory" && criteria.isValue == AssessmentCategory.MATHEMATICS.toString()
            } != null

            assert query.sort.getOrderFor("takenDate").getDirection() == Sort.Direction.DESC

            return mongoFindCommand
        }

        then:
        1 * mongoFindCommand.execute() >> new Try.Success<List<UserAssessment>>([Mock(UserAssessment)])
        maybeUserAssessments.isSuccess()
    }

    def "getUserAssessmentsByCategory: success"(){
        setup:
        List<UserAssessment> userAssessmentList = [
                new CATUserAssessment(assessmentCategory: AssessmentCategory.MATHEMATICS),
                new CATUserAssessment(assessmentCategory: AssessmentCategory.READING),
                new WritingPromptUserAssessment(assessmentCategory: AssessmentCategory.WRITING),
                new MultipleChoiceUserAssessment(assessmentCategory: AssessmentCategory.COLLEGE_SKILLS),
                new MultipleChoiceUserAssessment(assessmentCategory: AssessmentCategory.COLLEGE_SKILLS),
                new MultipleChoiceUserAssessment(assessmentCategory: AssessmentCategory.COLLEGE_SKILLS)
        ]

        when:
        Try<Map<AssessmentCategory, List<UserAssessment>>> maybeUserAssessments = userAssessmentRepository.getUserAssessmentsByCategory(userId)

        then:
        1 * hystrixCommandFactory.getMongoFindCommand(*_) >> { arguments ->
            Query query = arguments[2]

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "userId" && criteria.isValue == userId
            } != null

            assert query.sort.getOrderFor("takenDate").getDirection() == Sort.Direction.DESC

            return mongoFindCommand
        }

        1 * mongoFindCommand.execute() >> new Try.Success<List<UserAssessment>>(userAssessmentList)

        then:
        maybeUserAssessments.isSuccess()
        maybeUserAssessments.get().size() == 4
    }

    def "getUserAssessmentsByCategory: mongoFindCommand fails, i fail"(){
        when:
        Try<Map<AssessmentCategory, List<UserAssessment>>> maybeUserAssessments = userAssessmentRepository.getUserAssessmentsByCategory(userId)

        then:
        1 * hystrixCommandFactory.getMongoFindCommand(*_) >> mongoFindCommand
        1 * mongoFindCommand.execute() >> new Try.Failure<List<UserAssessment>>(ioFailureTypeException)

        then:
        maybeUserAssessments.isFailure()
        maybeUserAssessments.failed().get() == ioFailureTypeException
    }

    def "getUserAssessments by category: mongoFindCommand fails, i fail"(){
        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessments(userId, AssessmentCategory.MATHEMATICS, null)

        then:
        1 * hystrixCommandFactory.getMongoFindCommand(*_) >> mongoFindCommand
        1 * mongoFindCommand.execute() >> new Try.Failure<List<UserAssessment>>(ioFailureTypeException)

        then:
        maybeUserAssessments.isFailure()
        maybeUserAssessments.failed().get() == ioFailureTypeException
    }

    def "getUserAssessments w/out takenDate: single assessmentId success"(){
        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessments(userId, assessmentId)

        then:
        1 * hystrixCommandFactory.getMongoFindCommand(*_) >> { arguments ->
            Query query = arguments[2]

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "userId" && criteria.isValue == userId
            } != null

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "assessmentId" && criteria.isValue == assessmentId
            } != null

            assert query.sort.getOrderFor("takenDate").getDirection() == Sort.Direction.DESC

            return mongoFindCommand
        }

        then:
        1 * mongoFindCommand.execute() >> new Try.Success<List<UserAssessment>>([Mock(UserAssessment)])
        maybeUserAssessments.isSuccess()
    }

    def "getUserAssessments: multiple assessmentIds returns latest takenDate of each assessment if all assessmentIds return at least one assessment"(){
        setup:
        String assessmentId1 = UUID.randomUUID().toString()
        String assessmentId2 = UUID.randomUUID().toString()
        String assessmentId3 = UUID.randomUUID().toString()
        List<UserAssessment> dummyUserAssessments = [
                new CATUserAssessment( assessmentId: assessmentId1, takenDate: Instant.parse("2016-01-01T00:00:00.000Z")),
                new MultipleChoiceUserAssessment( assessmentId: assessmentId2, takenDate: Instant.parse("2015-01-01T00:00:00.000Z")),
                new WritingPromptUserAssessment( assessmentId: assessmentId3, takenDate: Instant.parse("2014-01-01T00:00:00.000Z")),
                new MultipleChoiceUserAssessment( assessmentId: assessmentId1, takenDate: Instant.parse("2013-01-01T00:00:00.000Z")),
                new CATUserAssessment( assessmentId: assessmentId2, takenDate: Instant.parse("2012-01-01T00:00:00.000Z")),
                new WritingPromptUserAssessment( assessmentId: assessmentId3, takenDate: Instant.parse("2011-01-01T00:00:00.000Z")),
                new CATUserAssessment( assessmentId: assessmentId1, takenDate: Instant.parse("2010-01-01T00:00:00.000Z")),
                new WritingPromptUserAssessment( assessmentId: assessmentId2, takenDate: Instant.parse("2009-01-01T00:00:00.000Z")),
                new MultipleChoiceUserAssessment( assessmentId: assessmentId3, takenDate: Instant.parse("2008-01-01T00:00:00.000Z"))
        ];

        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getLatestUserAssessments(userId, [assessmentId1, assessmentId2, assessmentId3])

        then:
        1 * mongoFindCommand.execute() >> new Try.Success<List<UserAssessment>>(dummyUserAssessments)
        maybeUserAssessments.isSuccess()

        then:
        maybeUserAssessments.get().size() == 3
        maybeUserAssessments.get().find { it.assessmentId == assessmentId1 && it.takenDate == Instant.parse("2016-01-01T00:00:00.000Z") } != null
        maybeUserAssessments.get().find { it.assessmentId == assessmentId2 && it.takenDate == Instant.parse("2015-01-01T00:00:00.000Z") } != null
        maybeUserAssessments.get().find { it.assessmentId == assessmentId3 && it.takenDate == Instant.parse("2014-01-01T00:00:00.000Z") } != null
    }

    def "getUserAssessments: multiple assessmentIds returns latest takenDate of each assessment when an invalid assessmentId is provided"(){
        setup:
        String assessmentId1 = UUID.randomUUID().toString()
        String assessmentId2 = UUID.randomUUID().toString()
        String assessmentId3 = UUID.randomUUID().toString()
        List<UserAssessment> dummyUserAssessments = [
                new CATUserAssessment( assessmentId: assessmentId1, takenDate: Instant.parse("2016-01-01T00:00:00.000Z")),
                new MultipleChoiceUserAssessment( assessmentId: assessmentId2, takenDate: Instant.parse("2015-01-01T00:00:00.000Z")),
                new MultipleChoiceUserAssessment( assessmentId: assessmentId1, takenDate: Instant.parse("2013-01-01T00:00:00.000Z")),
                new CATUserAssessment( assessmentId: assessmentId2, takenDate: Instant.parse("2012-01-01T00:00:00.000Z")),
                new CATUserAssessment( assessmentId: assessmentId1, takenDate: Instant.parse("2010-01-01T00:00:00.000Z")),
                new WritingPromptUserAssessment( assessmentId: assessmentId2, takenDate: Instant.parse("2009-01-01T00:00:00.000Z"))
        ];

        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getLatestUserAssessments(userId, [assessmentId1, assessmentId2, assessmentId3])

        then:
        1 * mongoFindCommand.execute() >> new Try.Success<List<UserAssessment>>(dummyUserAssessments)
        maybeUserAssessments.isSuccess()

        then:
        maybeUserAssessments.get().size() == 2
        maybeUserAssessments.get().find { it.assessmentId == assessmentId1 && it.takenDate == Instant.parse("2016-01-01T00:00:00.000Z") } != null
        maybeUserAssessments.get().find { it.assessmentId == assessmentId2 && it.takenDate == Instant.parse("2015-01-01T00:00:00.000Z") } != null
    }

    def "getUserAssessments: multiple assessmentIds mongo call fails, i fail"(){
        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getLatestUserAssessments(userId, [])

        then:
        1 * mongoFindCommand.execute() >> new Try.Failure<List<UserAssessment>>(ioFailureTypeException)
        maybeUserAssessments.isFailure()
    }

    def "saveUserAssessment: success"(){
        when:
        Try<Void> maybeResults = userAssessmentRepository.saveUserAssessment(Mock(UserAssessment))

        then:
        1 * mongoSaveCommand.execute() >> new Try.Success<Void>(null)
        maybeResults.isSuccess()
    }

    def "insertUserAssessment: success"(){
        setup:
        UserAssessment userAssessment = new CATUserAssessment(userId: "123", assessmentId: "123", takenDate: Instant.now())

        when:
        Try<Void> maybeResults = userAssessmentRepository.insertUserAssessment(userAssessment)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<UserAssessment>(null)

        then:
        1 * mongoInsertCommand.execute() >> new Try.Success<Void>(null)
        maybeResults.isSuccess()
    }

    def "insertUserAssessment: already exists"(){
        setup:
        UserAssessment userAssessment = new CATUserAssessment(userId: "123", assessmentId: "123", takenDate: Instant.now())

        when:
        Try<Void> maybeResults = userAssessmentRepository.insertUserAssessment(userAssessment)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<UserAssessment>(userAssessment)

        then:
        0 * mongoInsertCommand.execute()
        maybeResults.isFailure()
        maybeResults.failed().get() instanceof AlreadyExistsException
    }

    def "insertUserAssessment: io exception #1"(){
        setup:
        UserAssessment userAssessment = new CATUserAssessment(userId: "123", assessmentId: "123", takenDate: Instant.now())

        when:
        Try<Void> maybeResults = userAssessmentRepository.insertUserAssessment(userAssessment)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Failure<UserAssessment>(ioFailureTypeException)

        then:
        0 * mongoInsertCommand.execute()
        maybeResults.isFailure()
    }

    def "insertUserAssessment: io exception #2"(){
        setup:
        UserAssessment userAssessment = new CATUserAssessment(userId: "123", assessmentId: "123", takenDate: Instant.now())

        when:
        Try<Void> maybeResults = userAssessmentRepository.insertUserAssessment(userAssessment)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<UserAssessment>(null)

        then:
        1 * mongoInsertCommand.execute() >> new Try.Failure<Void>(ioFailureTypeException)
        maybeResults.isFailure()
    }

    def "getLatestUserAssessment: success"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getLatestUserAssessment(userId, assessmentId)

        then:
        1 * hystrixCommandFactory.getMongoFindOneCommand(*_) >> { arguments ->
            Query query = arguments[2]

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "userId" && criteria.isValue == userId
            } != null

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "assessmentId" && criteria.isValue == assessmentId
            } != null

            assert query.sort.getOrderFor("takenDate").getDirection() == Sort.Direction.DESC

            assert query.limit == 1

            return mongoFindOneCommand
        }

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<UserAssessment>(Mock(UserAssessment))
        maybeUserAssessment.isSuccess()
    }

    def "getLatestUserAssessment: failed, return failure"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getLatestUserAssessment(userId, assessmentId)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Failure<UserAssessment>(new RepoNotFoundException("not found"))
        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof RepoNotFoundException
    }

    def "getLatestUserAssessment: success w/null, return failure"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getLatestUserAssessment(userId, assessmentId)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<UserAssessment>(null)
        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof RepoNotFoundException
    }

    def "getUserAssessmentById: success"(){
        setup:
        UserAssessment userAssessment = new CATUserAssessment(userId: userId)

        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getUserAssessmentById(userId, userAssessmentId)

        then:
        1 * hystrixCommandFactory.getMongoFindByIdCommand(_, _, userAssessmentId, _) >> mongoFindByIdCommand

        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Success<UserAssessment>(userAssessment)
        maybeUserAssessment.isSuccess()
    }

    def "getUserAssessmentById: fails if userids do not match"(){
        setup:
        UserAssessment userAssessment = new CATUserAssessment(userId: "123")

        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getUserAssessmentById(userId, userAssessmentId)
        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Success<UserAssessment>(userAssessment)
        maybeUserAssessment.isFailure()
    }

    def "getUserAssessmentById: failed, return failure"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getUserAssessmentById(userId, userAssessmentId)

        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Failure<UserAssessment>(new RepoNotFoundException("not found"))
        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof RepoNotFoundException
    }

    def "getUserAssessmentById: success w/null, return failure"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getUserAssessmentById(userId, userAssessmentId)

        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Success<UserAssessment>(null)
        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof RepoNotFoundException
    }

    def "getLatestUserAssessment (2): success"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getLatestUserAssessment(userId)

        then:
        1 * hystrixCommandFactory.getMongoFindOneCommand(*_) >> { arguments ->
            Query query = arguments[2]

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "userId" && criteria.isValue == userId
            } != null

            return mongoFindOneCommand
        }

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<UserAssessment>(Mock(UserAssessment))
        maybeUserAssessment.isSuccess()
    }

    def "getLatestUserAssessment (2): failed, return failure"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getLatestUserAssessment(userId)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Failure<UserAssessment>(new RepoNotFoundException("not found"))
        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof RepoNotFoundException
    }

    def "getLatestUserAssessment (2): success w/null, return failure"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getLatestUserAssessment(userId)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<UserAssessment>(null)
        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof RepoNotFoundException
    }

    def "getUserAssessments (by status): success"(){
        setup:
        List<CompletionStatus> statuses = [CompletionStatus.COMPLETED, CompletionStatus.GRADING_FAILURE]
        List<ScoringType> scoringTypes = [ScoringType.AVERAGE, ScoringType.SUM]

        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessments(statuses, scoringTypes, userId, 10, 5);

        then:
        1 * hystrixCommandFactory.getMongoFindCommand(*_) >> { arguments ->
            Query query = arguments[2]

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "userId" && criteria.isValue == userId
            } != null

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "status" &&
                        criteria.criteria.get("\$in").contains(CompletionStatus.COMPLETED) &&
                        criteria.criteria.get("\$in").contains(CompletionStatus.GRADING_FAILURE)
            } != null

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "scoringType" &&
                        criteria.criteria.get("\$in").contains(ScoringType.AVERAGE) &&
                        criteria.criteria.get("\$in").contains(ScoringType.SUM)
            } != null

            assert query.sort.getOrderFor("takenDate").getDirection() == Sort.Direction.DESC

            assert query.limit == 10
            assert query.skip == 5

            return mongoFindCommand
        }

        then:
        1 * mongoFindCommand.execute() >> new Try.Success<List<UserAssessment>>([])
        maybeUserAssessments.isSuccess()

    }

    def "getUserAssessments (by status): success, userid null"(){
        setup:
        List<CompletionStatus> statuses = [CompletionStatus.COMPLETED]
        List<ScoringType> scoringTypes = [ScoringType.AVERAGE, ScoringType.SUM]

        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessments(statuses, scoringTypes, null, 10, 5);

        then:
        1 * hystrixCommandFactory.getMongoFindCommand(*_) >> { arguments ->
            Query query = arguments[2]

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "userId"
            } == null

            return mongoFindCommand
        }

        then:
        1 * mongoFindCommand.execute() >> new Try.Success<List<UserAssessment>>([])
        maybeUserAssessments.isSuccess()
    }

    def "getUserAssessments (by status): success, scoringTypes null"(){
        setup:
        List<CompletionStatus> statuses = [CompletionStatus.COMPLETED]

        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessments(statuses, null, null, 10, 5);

        then:
        1 * hystrixCommandFactory.getMongoFindCommand(*_) >> { arguments ->
            Query query = arguments[2]

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "scoringType"
            } == null

            return mongoFindCommand
        }

        then:
        1 * mongoFindCommand.execute() >> new Try.Success<List<UserAssessment>>([])
        maybeUserAssessments.isSuccess()
    }

    def "getUserAssessments (by status): find command fails, i fail"(){
        setup:
        List<CompletionStatus> statuses = [CompletionStatus.COMPLETED]
        List<ScoringType> scoringTypes = [ScoringType.AVERAGE, ScoringType.SUM]

        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessments(statuses, scoringTypes, null, 10, 5);

        then:
        1 * mongoFindCommand.execute() >> new Try.Failure<List<UserAssessment>>(new Exception())
        maybeUserAssessments.isFailure()
    }

    def "getUserAssessments (by status, by assessmentid): success"(){
        setup:
        List<CompletionStatus> statuses = [CompletionStatus.COMPLETED, CompletionStatus.GRADING_FAILURE]
        List<String> assessmentIds = ["1", "2"]

        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessments(statuses, assessmentIds);

        then:
        1 * hystrixCommandFactory.getMongoFindCommand(*_) >> { arguments ->
            Query query = arguments[2]

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "status" &&
                        criteria.criteria.get("\$in").contains(CompletionStatus.COMPLETED) &&
                        criteria.criteria.get("\$in").contains(CompletionStatus.GRADING_FAILURE)
            } != null

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "assessmentId" &&
                        criteria.criteria.get("\$in").contains("1") &&
                        criteria.criteria.get("\$in").contains("2")
            } != null

            assert query.sort.getOrderFor("takenDate").getDirection() == Sort.Direction.DESC

            return mongoFindCommand
        }

        then:
        1 * mongoFindCommand.execute() >> new Try.Success<List<UserAssessment>>([])
        maybeUserAssessments.isSuccess()

    }

    def "getUserAssessments (by status, by assessmentId): find command fails, i fail"(){
        setup:
        List<CompletionStatus> statuses = [CompletionStatus.COMPLETED]
        List<String> assessmentIds = ["1", "2"]

        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessments(statuses, assessmentIds);

        then:
        1 * mongoFindCommand.execute() >> new Try.Failure<List<UserAssessment>>(new Exception())
        maybeUserAssessments.isFailure()
    }

    def "getCompletedUserAssessmentSummaries: success"(){
        setup:
        Instant startDate = Instant.parse("2016-01-01T00:00:00.000Z");
        Instant endDate = Instant.parse("2016-01-01T00:00:00.000Z");

        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getCompletedUserAssessments(startDate, endDate);

        then:
        1 * hystrixCommandFactory.getMongoFindCommand(*_) >> { arguments ->
            Query query = arguments[2]

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "completionDate" &&
                        criteria.criteria.get("\$gte") == Date.from(startDate) &&
                        criteria.criteria.get("\$lt") == Date.from(endDate)
            } != null

            assert query.sort.getOrderFor("completionDate").getDirection() == Sort.Direction.DESC

            return mongoFindCommand
        }

        then:
        1 * mongoFindCommand.execute() >> new Try.Success<List<UserAssessment>>([])
        maybeUserAssessments.isSuccess()
    }

    def "getCompletedUserAssessmentSummaries: find command fails, i fail"(){
        setup:
        Instant startDate = Instant.parse("2016-01-01T00:00:00.000Z");
        Instant endDate = Instant.parse("2016-01-01T00:00:00.000Z");

        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getCompletedUserAssessments(startDate, endDate);

        then:
        1 * mongoFindCommand.execute() >> new Try.Failure<List<UserAssessment>>(new Exception())
        maybeUserAssessments.isFailure()
    }

    def "getLatestUserAssessment 2: success"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getLatestUserAssessment(userId, AssessmentCategory.MATHEMATICS)

        then:
        1 * hystrixCommandFactory.getMongoFindOneCommand(*_) >> { arguments ->
            Query query = arguments[2]

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "userId" && criteria.isValue == userId
            } != null

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "assessmentCategory" && criteria.isValue == AssessmentCategory.MATHEMATICS.toString()
            } != null

            assert query.sort.getOrderFor("takenDate").getDirection() == Sort.Direction.DESC

            assert query.limit == 1

            return mongoFindOneCommand
        }

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<UserAssessment>(Mock(UserAssessment))
        maybeUserAssessment.isSuccess()
    }

    def "getLatestUserAssessment 2: failed, return failure"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getLatestUserAssessment(userId, AssessmentCategory.MATHEMATICS)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Failure<UserAssessment>(new RepoNotFoundException("not found"))
        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof RepoNotFoundException
    }

    def "getLatestUserAssessment 2: success w/null, return failure"(){
        when:
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getLatestUserAssessment(userId, AssessmentCategory.MATHEMATICS)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<UserAssessment>(null)
        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof RepoNotFoundException
    }

    def "getUserAssessments by userId: success"(){
        when:
        Try<List<UserAssessment>> maybeResults = userAssessmentRepository.getUserAssessments("123")

        then:
        1 * hystrixCommandFactory.getMongoFindCommand(_, _, _, UserAssessment.class) >> { args ->
            Query query = args[2]
            assert query.criteria.find { Criteria criteria ->
                criteria.key == "userId" && criteria.isValue == "123"
            } != null

            assert query.sort.getOrderFor("takenDate").getDirection() == Sort.Direction.DESC

            return mongoFindCommand
        }
        1 * mongoFindCommand.execute() >> new Try.Success<List<UserAssessment>>([Mock(UserAssessment)])

        then:
        maybeResults.isSuccess()
        maybeResults.get().size() == 1
    }

    def "getUserAssessments by userId: mongoFindCommand fails, i fail"(){
        when:
        Try<List<UserAssessment>> maybeResults = userAssessmentRepository.getUserAssessments("123")

        then:
        1 * hystrixCommandFactory.getMongoFindCommand(_, _, _, UserAssessment.class) >> mongoFindCommand
        1 * mongoFindCommand.execute() >> new Try.Failure<List<UserAssessment>>(new Exception())

        then:
        maybeResults.isFailure()
    }

    @Unroll
    def "getUserAssessments by category and completion status - category test: success"(AssessmentCategory[] assessmentCategories){
        setup:

        Instant startDate = Instant.parse("2016-01-01T00:00:00.000Z");
        Instant endDate = Instant.parse("2016-01-01T00:00:00.000Z");

        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessments(assessmentCategories, CompletionStatus.GRADED, startDate, endDate);

        then:
        1 * hystrixCommandFactory.getMongoFindCommand(*_) >> { arguments ->
            Query query = arguments[2]

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "takenDate" &&
                        criteria.criteria.get("\$gte") == Date.from(startDate) &&
                        criteria.criteria.get("\$lt") == Date.from(endDate)
            } != null

            assert query.sort.getOrderFor("takenDate").getDirection() == Sort.Direction.DESC
            return mongoFindCommand
        }

        then:
        1 * mongoFindCommand.execute() >> new Try.Success<List<UserAssessment>>([])
        maybeUserAssessments.isSuccess()

        where:
        assessmentCategories << [[AssessmentCategory.MATHEMATICS, AssessmentCategory.READING], [], null]

    }

    @Unroll
    def "getUserAssessments by category and completion status - completion status test: success"(){
        setup:

        Instant startDate = Instant.parse("2016-01-01T00:00:00.000Z");
        Instant endDate = Instant.parse("2016-01-01T00:00:00.000Z");
        AssessmentCategory[] assessmentCategories = []

        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessments(assessmentCategories, completionStatus, startDate, endDate);

        then:
        1 * hystrixCommandFactory.getMongoFindCommand(*_) >> { arguments ->
            Query query = arguments[2]

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "takenDate" &&
                        criteria.criteria.get("\$gte") == Date.from(startDate) &&
                        criteria.criteria.get("\$lt") == Date.from(endDate)
            } != null

            assert query.sort.getOrderFor("takenDate").getDirection() == Sort.Direction.DESC
            return mongoFindCommand
        }

        then:
        1 * mongoFindCommand.execute() >> new Try.Success<List<UserAssessment>>([])
        maybeUserAssessments.isSuccess()

        where:
        completionStatus | _
        CompletionStatus.GRADED | _
        CompletionStatus.COMPLETED| _

    }

    def "getUserAssessments by category and completion status: find command fails, i fail"(){
        setup:
        Instant startDate = Instant.parse("2016-01-01T00:00:00.000Z");
        Instant endDate = Instant.parse("2016-01-01T00:00:00.000Z");
        AssessmentCategory[] assessmentCategories = [AssessmentCategory.MATHEMATICS];
        CompletionStatus completionStatus = CompletionStatus.GRADED;

        when:
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessments(assessmentCategories, completionStatus, startDate, endDate);

        then:
        1 * mongoFindCommand.execute() >> new Try.Failure<List<UserAssessment>>(new Exception())
        maybeUserAssessments.isFailure()
    }
}
