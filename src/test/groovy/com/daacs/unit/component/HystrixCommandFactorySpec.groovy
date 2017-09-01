package com.daacs.component

import com.daacs.framework.hystrix.GavantHystrixCommand
import com.daacs.model.User
import com.daacs.repository.hystrix.*
import com.daacs.service.hystrix.*
import com.daacs.service.hystrix.http.CanvasUpdateSubmissionHystrixCommand
import org.springframework.data.mongodb.core.CollectionOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.mail.javamail.JavaMailSender
import spock.lang.Specification

import javax.mail.internet.MimeMessage
import java.nio.file.Paths

/**
 * Created by chostetter on 6/22/16.
 */
class HystrixCommandFactorySpec extends Specification {

    HystrixCommandFactory hystrixCommandFactory
    def setup(){
        hystrixCommandFactory = new HystrixCommandFactory()
    }

    def "getMongoFindByIdCommand returns MongoFindByIdCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getMongoFindByIdCommand("test", Mock(MongoTemplate), "1", User.class)

        then:
        gavantHystrixCommand instanceof MongoFindByIdCommand
    }

    def "getMongoFindCommand returns MongoFindCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getMongoFindCommand("test", Mock(MongoTemplate), new Query(), User.class)

        then:
        gavantHystrixCommand instanceof MongoFindCommand
    }

    def "getMongoFindOneCommand returns MongoFindOneCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getMongoFindOneCommand("test", Mock(MongoTemplate), new Query(), User.class)

        then:
        gavantHystrixCommand instanceof MongoFindOneCommand
    }

    def "getMongoInsertCommand returns MongoInsertCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getMongoInsertCommand("test", Mock(MongoTemplate), new User())

        then:
        gavantHystrixCommand instanceof MongoInsertCommand
    }

    def "getMongoSaveCommand returns MongoSaveCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getMongoSaveCommand("test", Mock(MongoTemplate), new User())

        then:
        gavantHystrixCommand instanceof MongoSaveCommand
    }

    def "getSendMailHystrixCommand returns SendMailHystrixCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getSendMailHystrixCommand("test", Mock(JavaMailSender), Mock(MimeMessage))

        then:
        gavantHystrixCommand instanceof SendMailHystrixCommand
    }

    def "getMongoAggregateCommand returns MongoAggregateCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getMongoAggregateCommand("test", Mock(MongoTemplate), Mock(Aggregation), String.class, String.class)

        then:
        gavantHystrixCommand instanceof MongoAggregateCommand
    }

    def "getMongoCreateCollectionCommand returns MongoCreateCollectionCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getMongoCreateCollectionCommand("test", Mock(MongoTemplate), "collection", Mock(CollectionOptions))

        then:
        gavantHystrixCommand instanceof MongoCreateCollectionCommand
    }

    def "getMongoGetCollectionCommand returns MongoGetCollectionCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getMongoGetCollectionCommand("test", Mock(MongoTemplate), "collection")

        then:
        gavantHystrixCommand instanceof MongoGetCollectionCommand
    }

    def "getMongoGetTailableCursorCommand returns MongoTailableCursorCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getMongoTailableCursorCommand("test", Mock(MongoTemplate), "collection")

        then:
        gavantHystrixCommand instanceof MongoTailableCursorCommand
    }

    def "getMongoCollectionExistsCommand returns MongoCollectionExistsCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getMongoCollectionExistsCommand("test", Mock(MongoTemplate), "collection")

        then:
        gavantHystrixCommand instanceof MongoCollectionExistsCommand
    }

    def "getMongoInsertByCollectionCommand returns MongoInsertByCollectionCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getMongoInsertByCollectionCommand("test", Mock(MongoTemplate), new User(), "collection")

        then:
        gavantHystrixCommand instanceof MongoInsertByCollectionCommand
    }

    def "getExecuteCommandHystrixCommand returns ExecuteCommandHystrixCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getExecuteCommandHystrixCommand("test", "cd /", null, Paths.get("/").toFile())

        then:
        gavantHystrixCommand instanceof ExecuteCommandHystrixCommand
    }

    def "getWriteCsvHystrixCommand returns WriteCsvHystrixCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getWriteCsvHystrixCommand("test", Paths.get("/test.csv"), [])

        then:
        gavantHystrixCommand instanceof WriteCsvHystrixCommand
    }

    def "getReadCsvHystrixCommand returns ReadCsvHystrixCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getReadCsvHystrixCommand("test", Paths.get("/test.csv"))

        then:
        gavantHystrixCommand instanceof ReadCsvHystrixCommand
    }

    def "getLightSideDirCheckHystrixCommand returns LightSideDirCheckHystrixCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getLightSideDirCheckHystrixCommand("test", Paths.get("/"), Paths.get("/"), Paths.get("/"))

        then:
        gavantHystrixCommand instanceof LightSideDirCheckHystrixCommand
    }

    def "getLightSideInputCheckHystrixCommand returns LightSideInputCheckHystrixCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getLightSideInputCheckHystrixCommand("test", Paths.get("/"), Paths.get("/"), Paths.get("/"))

        then:
        gavantHystrixCommand instanceof LightSideInputCheckHystrixCommand
    }

    def "getDeleteFileHystrixCommand returns DeleteFileHystrixCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getDeleteFileHystrixCommand("test", Paths.get("/"))

        then:
        gavantHystrixCommand instanceof DeleteFileHystrixCommand
    }

    def "getWriteFileHystrixCommand returns WriteFileHystrixCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getWriteFileHystrixCommand("test", new ByteArrayInputStream(), Paths.get("/"))

        then:
        gavantHystrixCommand instanceof WriteFileHystrixCommand
    }

    def "getMongoUpsertCommand returns MongoUpsertCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getMongoUpsertCommand("test", Mock(MongoTemplate), Mock(Query), Mock(Update), String.class)

        then:
        gavantHystrixCommand instanceof MongoUpsertCommand
    }

    def "getCanvasUpdateSubmissionHystrixCommand returns CanvasUpdateSubmissionHystrixCommand"(){
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getCanvasUpdateSubmissionHystrixCommand("test", 1, 1, "123", [])

        then:
        gavantHystrixCommand instanceof CanvasUpdateSubmissionHystrixCommand
    }

}
