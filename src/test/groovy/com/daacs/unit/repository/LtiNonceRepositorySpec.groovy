package com.daacs.unit.repository

import com.daacs.component.HystrixCommandFactory
import com.daacs.framework.exception.RepoNotFoundException
import com.daacs.model.Nonce
import com.daacs.repository.lti.LtiNonceRepository
import com.daacs.repository.lti.LtiNonceRepositoryImpl
import com.daacs.repository.hystrix.*
import com.lambdista.util.Try
import org.springframework.data.mongodb.core.MongoTemplate
import spock.lang.Specification
/**
 * Created by mgoldman on 4/10/19.
 */
class LtiNonceRepositorySpec extends Specification {

    LtiNonceRepository nonceRepository

    HystrixCommandFactory hystrixCommandFactory
    MongoFindByIdCommand mongoFindByIdCommand
    MongoInsertCommand  mongoInsertCommand
    MongoTemplate mongoTemplate

    Nonce dummyNonce

    def setup(){

        mongoFindByIdCommand = Mock(MongoFindByIdCommand)
        mongoInsertCommand = Mock(MongoInsertCommand)

        mongoTemplate = Mock(MongoTemplate)
        hystrixCommandFactory = Mock(HystrixCommandFactory)
        hystrixCommandFactory.getMongoFindByIdCommand(*_) >> mongoFindByIdCommand
        hystrixCommandFactory.getMongoInsertCommand(*_) >> mongoInsertCommand

        nonceRepository = new LtiNonceRepositoryImpl(hystrixCommandFactory: hystrixCommandFactory, mongoTemplate: mongoTemplate)

        dummyNonce = new Nonce("nonceId")
    }

    def "getNonce: success"(){
        when:
        Try<Nonce> maybeNonce = nonceRepository.getNonce("nonceId")

        then:
        1 * hystrixCommandFactory.getMongoFindByIdCommand(*_) >> { arguments ->
            return mongoFindByIdCommand
        }

        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Success<Nonce>(dummyNonce)
        maybeNonce.isSuccess()
    }

    def "getNonce: failed, return failure"(){
        when:
        Try<Nonce> maybeNonce = nonceRepository.getNonce("nonceId")

        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Failure<Nonce>(new RepoNotFoundException("not found"))
        maybeNonce.isFailure()
        maybeNonce.failed().get() instanceof RepoNotFoundException
    }

    def "insertNonce: success"(){
        when:
        Try<Nonce> maybeNonce = nonceRepository.insertNonce(dummyNonce)


        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Success<Nonce>(null)

        1 * hystrixCommandFactory.getMongoInsertCommand(*_) >> { arguments ->
            return mongoInsertCommand
        }

        then:
        1 * mongoInsertCommand.execute() >> new Try.Success<Nonce>(dummyNonce)
        maybeNonce.isSuccess()
    }

    def "insertNonce: failure, already exists"(){
        when:
        Try<Nonce> maybeNonce = nonceRepository.insertNonce(dummyNonce)


        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Success<Nonce>(dummyNonce)

        0 * hystrixCommandFactory.getMongoInsertCommand(*_) >> { arguments ->
            return mongoInsertCommand
        }

        then:
        0 * mongoInsertCommand.execute() >> new Try.Success<Nonce>(dummyNonce)
        maybeNonce.isFailure()
    }


    def "insertNonce: failure on get"(){
        when:
        Try<Nonce> maybeNonce = nonceRepository.insertNonce(dummyNonce)


        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Failure<Nonce>(new RepoNotFoundException("not found"))

        0 * hystrixCommandFactory.getMongoInsertCommand(*_) >> { arguments ->
            return mongoInsertCommand
        }

        then:
        0 * mongoInsertCommand.execute() >> new Try.Success<Nonce>(dummyNonce)
        maybeNonce.isFailure()
    }

}
