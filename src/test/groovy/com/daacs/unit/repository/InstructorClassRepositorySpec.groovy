package com.daacs.unit.repository

import com.daacs.component.HystrixCommandFactory
import com.daacs.framework.exception.RepoNotFoundException
import com.daacs.model.InstructorClass
import com.daacs.repository.InstructorClassRepositoryImpl
import com.daacs.repository.InstructorClassRepository
import com.daacs.repository.hystrix.*
import com.lambdista.util.Try
import spock.lang.Specification

/**
 * Created by mgoldman on 6/9/20s.
 */
class InstructorClassRepositorySpec extends Specification {

    InstructorClassRepository instructorClassRepository

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

        instructorClassRepository = new InstructorClassRepositoryImpl(hystrixCommandFactory: hystrixCommandFactory)
    }

    def "getInstructorClass: success"(){
        when:
        Try<InstructorClass> maybeInstructorClass = instructorClassRepository.getClass(UUID.randomUUID().toString())

        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Success<InstructorClass>(Mock(InstructorClass))
        maybeInstructorClass.isSuccess()
    }

    def "getInstructorClass: failed, return failure"(){
        when:
        Try<InstructorClass> maybeInstructorClass = instructorClassRepository.getClass(UUID.randomUUID().toString())

        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Failure<InstructorClass>(new RepoNotFoundException("not found"))
        maybeInstructorClass.isFailure()
        maybeInstructorClass.failed().get() instanceof RepoNotFoundException
    }

    def "saveInstructorClass: success"(){
        when:
        Try<InstructorClass> maybeInstructorClass = instructorClassRepository.saveClass(Mock(InstructorClass))

        then:
        1 * mongoSaveCommand.execute() >> new Try.Success<Void>(null)
        maybeInstructorClass.isSuccess()
    }

    def "saveInstructorClass: failed, return failure"(){
        when:
        Try<InstructorClass> maybeInstructorClass = instructorClassRepository.saveClass(Mock(InstructorClass))

        then:
        1 * mongoSaveCommand.execute() >> new Try.Failure<Void>(null)
        maybeInstructorClass.isFailure()
    }

    def "getInstructorClass: success w/null, return failure"(){
        when:
        Try<InstructorClass> maybeInstructorClass = instructorClassRepository.getClass(UUID.randomUUID().toString())

        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Success<InstructorClass>(null)
        maybeInstructorClass.isFailure()
        maybeInstructorClass.failed().get() instanceof RepoNotFoundException
    }

    def "insertInstructorClass: success"(){
        when:
        Try<Void> maybeResults = instructorClassRepository.insertClass(Mock(InstructorClass))

        then:
        1 * mongoInsertCommand.execute() >> new Try.Success<Void>(null)
        maybeResults.isSuccess()
    }

    def "insertInstructorClass: failure"(){
        when:
        Try<Void> maybeResults = instructorClassRepository.insertClass(Mock(InstructorClass))

        then:
        1 * mongoInsertCommand.execute() >> new Try.Failure<Void>(null)
        maybeResults.isFailure()
    }

}
