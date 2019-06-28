package com.daacs.component;


import com.daacs.repository.hystrix.*;
import com.daacs.service.hystrix.*;
import com.daacs.service.hystrix.http.CanvasUpdateSubmissionHystrixCommand;
import org.apache.http.NameValuePair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Created by chostetter on 6/22/16.
 */
@Component
public class HystrixCommandFactory {

    @Value("${canvas.baseURL}")
    private String canvasBaseURL;

    @Value("${canvas.oAuthToken}")
    private String canvasOAuthToken;

    private static final String hystrixGroupKey = "daacs";

    public <T> MongoFindOneCommand<T> getMongoFindOneCommand(String hystrixCommandKey, MongoTemplate mongoTemplate, Query query, Class<T> entityClass){
        return new MongoFindOneCommand<>(hystrixGroupKey, hystrixCommandKey, mongoTemplate, query, entityClass);
    }

    public <T> MongoFindByIdCommand<T> getMongoFindByIdCommand(String hystrixCommandKey, MongoTemplate mongoTemplate, String id, Class<T> entityClass){
        return new MongoFindByIdCommand<>(hystrixGroupKey, hystrixCommandKey, mongoTemplate, id, entityClass);
    }

    public <T> MongoSaveCommand<T> getMongoSaveCommand(String hystrixCommandKey, MongoTemplate mongoTemplate, T entity){
        return new MongoSaveCommand<>(hystrixGroupKey, hystrixCommandKey, mongoTemplate, entity);
    }

    public <T> MongoInsertCommand<T> getMongoInsertCommand(String hystrixCommandKey, MongoTemplate mongoTemplate, T entity){
        return new MongoInsertCommand<>(hystrixGroupKey, hystrixCommandKey, mongoTemplate, entity);
    }

    public <T> MongoDeleteByIdCommand<T> getMongoDeleteByIdCommand(String hystrixCommandKey, MongoTemplate mongoTemplate, Query query, Class<T> entityClass){
        return new MongoDeleteByIdCommand<>(hystrixGroupKey, hystrixCommandKey, mongoTemplate, query, entityClass);
    }

    public <T> MongoFindCommand<T> getMongoFindCommand(String hystrixCommandKey, MongoTemplate mongoTemplate, Query query, Class<T> entityClass){
        return new MongoFindCommand<>(hystrixGroupKey, hystrixCommandKey, mongoTemplate, query, entityClass);
    }

    public SendMailHystrixCommand getSendMailHystrixCommand(String hystrixCommandKey, JavaMailSender javaMailSender, MimeMessage mail){
        return new SendMailHystrixCommand(hystrixGroupKey, hystrixCommandKey, javaMailSender, mail);
    }

    public <T> MongoAggregateCommand<T> getMongoAggregateCommand(String hystrixCommandKey, MongoTemplate mongoTemplate, Aggregation aggregation, Class<?> inputEntityClass, Class<T> outputEntityClass){
        return new MongoAggregateCommand<>(hystrixGroupKey, hystrixCommandKey, mongoTemplate, aggregation, inputEntityClass, outputEntityClass);
    }

    public MongoCreateCollectionCommand getMongoCreateCollectionCommand(String hystrixCommandKey, MongoTemplate mongoTemplate, String collectionName, CollectionOptions collectionOptions){
        return new MongoCreateCollectionCommand(hystrixGroupKey, hystrixCommandKey, mongoTemplate, collectionName, collectionOptions);
    }

    public MongoGetCollectionCommand getMongoGetCollectionCommand(String hystrixCommandKey, MongoTemplate mongoTemplate, String collectionName){
        return new MongoGetCollectionCommand(hystrixGroupKey, hystrixCommandKey, mongoTemplate, collectionName);
    }

    public MongoTailableCursorCommand getMongoTailableCursorCommand(String hystrixCommandKey, MongoTemplate mongoTemplate, String collectionName){
        return new MongoTailableCursorCommand(hystrixGroupKey, hystrixCommandKey, mongoTemplate, collectionName);
    }

    public MongoCollectionExistsCommand getMongoCollectionExistsCommand(String hystrixCommandKey, MongoTemplate mongoTemplate, String collectionName){
        return new MongoCollectionExistsCommand(hystrixGroupKey, hystrixCommandKey, mongoTemplate, collectionName);
    }

    public <T> MongoInsertByCollectionCommand<T> getMongoInsertByCollectionCommand(String hystrixCommandKey, MongoTemplate mongoTemplate, T entity, String collectionName){
        return new MongoInsertByCollectionCommand<>(hystrixGroupKey, hystrixCommandKey, mongoTemplate, entity, collectionName);
    }

    public ExecuteCommandHystrixCommand getExecuteCommandHystrixCommand(String hystrixCommandKey, String command, String[] envVars, File workingDir){
        return new ExecuteCommandHystrixCommand(hystrixGroupKey, hystrixCommandKey, command, envVars, workingDir);
    }

    public WriteCsvHystrixCommand getWriteCsvHystrixCommand(String hystrixCommandKey, Path file, List<String[]> writableLines){
        return new WriteCsvHystrixCommand(hystrixGroupKey, hystrixCommandKey, file, writableLines);
    }

    public ReadCsvHystrixCommand getReadCsvHystrixCommand(String hystrixCommandKey, Path file){
        return new ReadCsvHystrixCommand(hystrixGroupKey, hystrixCommandKey, file);
    }

    public LightSideDirCheckHystrixCommand getLightSideDirCheckHystrixCommand(String hystrixCommandKey, Path lightSideModelsDir, Path lightSideOutputDir, Path predictScript){
        return new LightSideDirCheckHystrixCommand(hystrixGroupKey, hystrixCommandKey, lightSideModelsDir, lightSideOutputDir, predictScript);
    }

    public LightSideInputCheckHystrixCommand getLightSideInputCheckHystrixCommand(String hystrixCommandKey, Path modelFile, Path inputFile, Path outputFile){
        return new LightSideInputCheckHystrixCommand(hystrixGroupKey, hystrixCommandKey, modelFile, inputFile, outputFile);
    }

    public DeleteFileHystrixCommand getDeleteFileHystrixCommand(String hystrixCommandKey, Path file){
        return new DeleteFileHystrixCommand(hystrixGroupKey, hystrixCommandKey, file);
    }

    public WriteFileHystrixCommand getWriteFileHystrixCommand(String hystrixCommandKey, InputStream inputStream, Path file){
        return new WriteFileHystrixCommand(hystrixGroupKey, hystrixCommandKey, inputStream, file);
    }

    public MongoUpsertCommand getMongoUpsertCommand(String hystrixCommandKey, MongoTemplate mongoTemplate, Query query, Update update, Class<?> entityClass){
        return new MongoUpsertCommand(hystrixGroupKey, hystrixCommandKey, mongoTemplate, query, update, entityClass);
    }

    public CanvasUpdateSubmissionHystrixCommand getCanvasUpdateSubmissionHystrixCommand(String hystrixCommandKey, Integer courseId, Integer assessmentId, String sisId, List<NameValuePair> params){
        return new CanvasUpdateSubmissionHystrixCommand(hystrixGroupKey, hystrixCommandKey, canvasBaseURL, canvasOAuthToken, courseId, assessmentId, sisId, params);
    }

    public LtiReplaceResultCommand getLTIReplaceResultCommand(String hystrixCommandKey, String lis_outcome_service_url, String oauth_consumer_key, String secretKey, String lis_result_sourcedid, String score){
        return new LtiReplaceResultCommand(hystrixGroupKey, hystrixCommandKey, lis_outcome_service_url, oauth_consumer_key, secretKey, lis_result_sourcedid, score);
    }
}
