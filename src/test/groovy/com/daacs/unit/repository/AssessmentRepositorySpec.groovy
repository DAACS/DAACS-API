package com.daacs.repository

import com.daacs.component.HystrixCommandFactory
import com.daacs.framework.exception.RepoNotFoundException
import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.AssessmentCategory
import com.daacs.model.assessment.ScoringType
import com.daacs.repository.hystrix.*
import com.lambdista.util.Try
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.GroupOperation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import spock.lang.Specification
import spock.lang.Unroll
/**
 * Created by chostetter on 6/22/16.
 */
class AssessmentRepositorySpec extends Specification {

    AssessmentRepository assessmentRepository

    HystrixCommandFactory hystrixCommandFactory
    MongoFindByIdCommand mongoFindByIdCommand
    MongoInsertCommand mongoInsertCommand
    MongoFindCommand mongoFindCommand
    MongoSaveCommand mongoSaveCommand
    MongoAggregateCommand mongoAggregateCommand

    def setup(){

        mongoFindByIdCommand = Mock(MongoFindByIdCommand)
        mongoInsertCommand = Mock(MongoInsertCommand)
        mongoSaveCommand = Mock(MongoSaveCommand)
        mongoFindCommand = Mock(MongoFindCommand)
        mongoAggregateCommand = Mock(MongoAggregateCommand)

        hystrixCommandFactory = Mock(HystrixCommandFactory)
        hystrixCommandFactory.getMongoFindByIdCommand(*_) >> mongoFindByIdCommand
        hystrixCommandFactory.getMongoInsertCommand(*_) >> mongoInsertCommand
        hystrixCommandFactory.getMongoFindCommand(*_) >> mongoFindCommand
        hystrixCommandFactory.getMongoSaveCommand(*_) >> mongoSaveCommand
        hystrixCommandFactory.getMongoAggregateCommand(*_) >> mongoAggregateCommand

        assessmentRepository = new AssessmentRepositoryImpl(hystrixCommandFactory: hystrixCommandFactory)
    }

    def "getAssessment: success"(){
        when:
        Try<Assessment> maybeAssessment = assessmentRepository.getAssessment(UUID.randomUUID().toString())

        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Success<Assessment>(Mock(Assessment))
        maybeAssessment.isSuccess()
    }

    def "getAssessment: failed, return failure"(){
        when:
        Try<Assessment> maybeAssessment = assessmentRepository.getAssessment(UUID.randomUUID().toString())

        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Failure<Assessment>(new RepoNotFoundException("not found"))
        maybeAssessment.isFailure()
        maybeAssessment.failed().get() instanceof RepoNotFoundException
    }

    def "saveAssessment: success"(){
        when:
        Try<Assessment> maybeAssessment = assessmentRepository.saveAssessment(Mock(Assessment))

        then:
        1 * mongoSaveCommand.execute() >> new Try.Success<Void>(null)
        maybeAssessment.isSuccess()
    }

    def "saveAssessment: failed, return failure"(){
        when:
        Try<Assessment> maybeAssessment = assessmentRepository.saveAssessment(Mock(Assessment))

        then:
        1 * mongoSaveCommand.execute() >> new Try.Failure<Void>(null)
        maybeAssessment.isFailure()
    }

    def "getAssessment: success w/null, return failure"(){
        when:
        Try<Assessment> maybeAssessment = assessmentRepository.getAssessment(UUID.randomUUID().toString())

        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Success<Assessment>(null)
        maybeAssessment.isFailure()
        maybeAssessment.failed().get() instanceof RepoNotFoundException
    }

    def "insertAssessment: success"(){
        when:
        Try<Void> maybeResults = assessmentRepository.insertAssessment(Mock(Assessment))

        then:
        1 * mongoInsertCommand.execute() >> new Try.Success<Void>(null)
        maybeResults.isSuccess()
    }

    @Unroll
    def "getAssessments: enabled success"(Boolean enabled){
        when:
        Try<List<Assessment>> maybeAssessments = assessmentRepository.getAssessments(enabled, Arrays.asList(AssessmentCategory.class.getEnumConstants()))

        then:
        1 * hystrixCommandFactory.getMongoFindCommand(*_) >> { arguments ->
            Query query = arguments[2]

            boolean foundCriteria = query.criteria.find { Criteria criteria ->
                criteria.key == "enabled" && criteria.isValue == enabled
            }

            if(enabled != null) {
                assert foundCriteria;
            }
            else{
                assert !foundCriteria;
            }

            return mongoFindCommand
        }

        then:
        1 * mongoFindCommand.execute() >> new Try.Success<List<Assessment>>([Mock(Assessment)])
        maybeAssessments.isSuccess()

        where:
        enabled << [true, false, null]
    }

    @Unroll
    def "getAssessments (by ScoringType): enabled success"(Boolean enabled){
        when:
        Try<List<Assessment>> maybeAssessments = assessmentRepository.getAssessments([ScoringType.AVERAGE, ScoringType.MANUAL], enabled)

        then:
        1 * hystrixCommandFactory.getMongoFindCommand(*_) >> { arguments ->
            Query query = arguments[2]

            boolean foundCriteria = query.criteria.find { Criteria criteria ->
                criteria.key == "enabled" && criteria.isValue == enabled
            }

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "scoringType" &&
                        criteria.criteria.get("\$in").contains(ScoringType.AVERAGE) &&
                        criteria.criteria.get("\$in").contains(ScoringType.MANUAL)
            } != null

            if(enabled != null) {
                assert foundCriteria;
            }
            else{
                assert !foundCriteria;
            }

            return mongoFindCommand
        }

        then:
        1 * mongoFindCommand.execute() >> new Try.Success<List<Assessment>>([Mock(Assessment)])
        maybeAssessments.isSuccess()

        where:
        enabled << [true, false, null]
    }

    def "getAssessments: failed, return failure"(){
        when:
        Try<Assessment> maybeAssessment = assessmentRepository.getAssessments([ScoringType.MANUAL], true)

        then:
        1 * mongoFindCommand.execute() >> new Try.Failure<Assessment>(new Exception())
        maybeAssessment.isFailure()
    }

    def "getAssessmentStats: success"(){
        when:
        Try<List<Map>> maybeResults = assessmentRepository.getAssessmentStats()

        then:
        1 * hystrixCommandFactory.getMongoAggregateCommand(*_) >> { arguments ->
            Aggregation agg = arguments[2]

            assert agg.operations.get(0) instanceof GroupOperation
            assert agg.operations.get(0).idFields.originalFields.find { it.field.name == "assessmentId" } != null
            assert agg.operations.get(0).idFields.originalFields.find { it.field.name == "status" } != null
            assert agg.operations.get(0).idFields.originalFields.find { it.field.name == "assessmentCategory" } != null

            agg.operations.get(0).operations.find { it.key == "total" } != null

            return mongoAggregateCommand
        }

        then:
        1 * mongoAggregateCommand.execute() >> new Try.Success<List<Map>>([[:]])

        then:
        maybeResults.isSuccess()
        maybeResults.get().size() == 1
    }

    def "getAssessmentStats: failed, return failure"(){
        when:
        Try<List<Map>> maybeStats = assessmentRepository.getAssessmentStats()

        then:
        1 * mongoAggregateCommand.execute() >> new Try.Failure<List<Map>>(new Exception())
        maybeStats.isFailure()
    }
}
