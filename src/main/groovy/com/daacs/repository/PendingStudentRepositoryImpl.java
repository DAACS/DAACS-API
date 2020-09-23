package com.daacs.repository;

import com.daacs.component.HystrixCommandFactory;
import com.daacs.framework.exception.AlreadyExistsException;
import com.daacs.model.PendingStudent;
import com.daacs.model.User;
import com.daacs.model.UserSearchResult;
import com.lambdista.util.Try;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.Fields;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;


@Repository
public class PendingStudentRepositoryImpl implements PendingStudentRepository {

    @Autowired
    private HystrixCommandFactory hystrixCommandFactory;

    @Autowired
    private MongoTemplate mongoTemplate;


    @Override
    public Try<PendingStudent>  getPendStudent(String username) {
        Query query = new Query();
        query.addCriteria(Criteria.where("username").is(username));

        Try<PendingStudent> maybeStudent = hystrixCommandFactory.getMongoFindOneCommand("PendingStudentRepositoryImpl-getPendStudent", mongoTemplate, query, PendingStudent.class).execute();

        if(maybeStudent.isFailure()){
            return maybeStudent;
        }

        //returns null if student doesn't exist
        if(!maybeStudent.toOptional().isPresent()){
            return new Try.Success<>(null);
        }

        return maybeStudent;
    }


    @Override
    public synchronized Try<Void> insertPendStudent(PendingStudent student) {
        Try<PendingStudent> maybeStudent = getPendStudent(student.getUsername());

        if(maybeStudent.isFailure()) {
            return new Try.Failure<>(maybeStudent.failed().get());
        }

        if(maybeStudent.toOptional().isPresent()){
            return new Try.Failure<>(new AlreadyExistsException("PendingStudent", "username", student.getUsername()));
        }

        return hystrixCommandFactory.getMongoInsertCommand("PendingStudentRepositoryImpl-insertPendStudent", mongoTemplate, student).execute();
    }

    @Override
    public Try<Void> deletePendStudent(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(id));
        return hystrixCommandFactory.getMongoDeleteByIdCommand("PendingStudentRepositoryImpl-deletePendStudent", mongoTemplate, query, PendingStudent.class).execute();
    }

    @Override
    public Try<Void> updatePendStudent(PendingStudent student) {
        return hystrixCommandFactory.getMongoSaveCommand(
                "PendingStudentRepositoryImpl-updatePendStudent", mongoTemplate, student).execute();
    }
}
