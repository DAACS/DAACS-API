package com.daacs.unit.repository

import com.daacs.component.HystrixCommandFactory
import com.daacs.framework.exception.AlreadyExistsException
import com.daacs.framework.exception.RepoNotFoundException
import com.daacs.model.InstructorClass
import com.daacs.model.PendingStudent
import com.daacs.model.User
import com.daacs.repository.InstructorClassRepository
import com.daacs.repository.InstructorClassRepositoryImpl
import com.daacs.repository.PendingStudentRepository
import com.daacs.repository.PendingStudentRepositoryImpl
import com.daacs.repository.hystrix.*
import com.lambdista.util.Try
import spock.lang.Specification

/**
 * Created by mgoldman
 */
class PendingStudentRepositorySpec extends Specification {

    PendingStudentRepository pendingStudentRepository

    PendingStudent dummyPendingStudent

    HystrixCommandFactory hystrixCommandFactory
    MongoFindByIdCommand mongoFindByIdCommand
    MongoInsertCommand mongoInsertCommand
    MongoFindOneCommand mongoFindOneCommand
    MongoDeleteByIdCommand mongoDeleteByIdCommand
    MongoSaveCommand mongoSaveCommand

    def setup(){

        dummyPendingStudent = new PendingStudent("username", "123", false)
        mongoFindByIdCommand = Mock(MongoFindByIdCommand)
        mongoInsertCommand = Mock(MongoInsertCommand)
        mongoFindOneCommand = Mock(MongoFindOneCommand)
        mongoDeleteByIdCommand = Mock(MongoDeleteByIdCommand)
        mongoSaveCommand = Mock(MongoSaveCommand)

        hystrixCommandFactory = Mock(HystrixCommandFactory)
        hystrixCommandFactory.getMongoFindByIdCommand(*_) >> mongoFindByIdCommand
        hystrixCommandFactory.getMongoInsertCommand(*_) >> mongoInsertCommand
        hystrixCommandFactory.getMongoFindOneCommand(*_) >> mongoFindOneCommand
        hystrixCommandFactory.getMongoDeleteByIdCommand(*_) >> mongoDeleteByIdCommand
        hystrixCommandFactory.getMongoSaveCommand(*_) >> mongoSaveCommand

        pendingStudentRepository = new PendingStudentRepositoryImpl(hystrixCommandFactory: hystrixCommandFactory)
    }

    def "getPendStudent: success"(){
        when:
        Try<PendingStudent> maybeStudent = pendingStudentRepository.getPendStudent(dummyPendingStudent.getUsername())

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<PendingStudent>(dummyPendingStudent)

        then:
        maybeStudent.isSuccess()
        maybeStudent.get().getUsername() == dummyPendingStudent.getUsername()
    }

    def "getPendStudent: failed, return failure"(){
        when:
        Try<PendingStudent> maybeStudent = pendingStudentRepository.getPendStudent(dummyPendingStudent.getUsername())

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Failure<PendingStudent>(new RepoNotFoundException("not found"))

        then:
        maybeStudent.isFailure()
        maybeStudent.failed().get() instanceof RepoNotFoundException
    }

    def "insertPendStudent: success"(){
        when:
        Try<Void> maybeResults = pendingStudentRepository.insertPendStudent(dummyPendingStudent)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<PendingStudent>(null)
        1 * mongoInsertCommand.execute() >> new Try.Success<Void>(null)

        then:
        maybeResults.isSuccess()
    }

    def "insertPendStudent: already exists"(){
        when:
        Try<Void> maybeResults = pendingStudentRepository.insertPendStudent(dummyPendingStudent)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<PendingStudent>(dummyPendingStudent)
        0 * mongoInsertCommand.execute() >> new Try.Success<Void>(null)

        then:
        maybeResults.isFailure()
        maybeResults.failed().get() instanceof AlreadyExistsException
    }

    def "insertPendStudent: mongoFindOneCommand fails"(){
        when:
        Try<Void> maybeResults = pendingStudentRepository.insertPendStudent(dummyPendingStudent)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Failure<PendingStudent>(null)
        0 * mongoInsertCommand.execute() >> new Try.Success<Void>(null)

        then:
        maybeResults.isFailure()
    }

    def "insertPendStudent: insert failure"(){
        when:
        Try<Void> maybeResults = pendingStudentRepository.insertPendStudent(dummyPendingStudent)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<PendingStudent>(null)
        1 * mongoInsertCommand.execute() >> new Try.Failure<Void>(null)

        then:
        maybeResults.isFailure()
    }

    def "deletePendStudent: success"(){
        when:
        Try<Void> maybeResult = pendingStudentRepository.deletePendStudent(dummyPendingStudent.getId())

        then:
        1 * mongoDeleteByIdCommand.execute() >> new Try.Success<Void>(null)

        then:
        maybeResult.isSuccess()
    }

    def "deletePendStudent: failure"(){
        when:
        Try<Void> maybeResult = pendingStudentRepository.deletePendStudent(dummyPendingStudent.getId())

        then:
        1 * mongoDeleteByIdCommand.execute() >> new Try.Failure<Void>()

        then:
        maybeResult.isFailure()
    }

    def "updatePendStudent: success"(){
        when:
        Try<Void> maybeResult = pendingStudentRepository.updatePendStudent(dummyPendingStudent)

        then:
        1 * mongoSaveCommand.execute() >> new Try.Success<Void>(null)

        then:
        maybeResult.isSuccess()
    }

    def "updatePendStudent: failure"(){
        when:
        Try<Void> maybeResult = pendingStudentRepository.updatePendStudent(dummyPendingStudent)

        then:
        1 * mongoSaveCommand.execute() >> new Try.Failure<Void>()

        then:
        maybeResult.isFailure()
    }

}
