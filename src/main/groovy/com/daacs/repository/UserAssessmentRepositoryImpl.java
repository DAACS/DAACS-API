package com.daacs.repository;

import com.daacs.component.HystrixCommandFactory;
import com.daacs.framework.exception.AlreadyExistsException;
import com.daacs.framework.exception.RepoNotFoundException;
import com.daacs.model.assessment.AssessmentCategory;
import com.daacs.model.assessment.ScoringType;
import com.daacs.model.assessment.user.CompletionStatus;
import com.daacs.model.assessment.user.UserAssessment;
import com.lambdista.util.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Created by chostetter on 7/7/16.
 */

@Repository
public class UserAssessmentRepositoryImpl implements UserAssessmentRepository {

    @Autowired
    private HystrixCommandFactory hystrixCommandFactory;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Try<UserAssessment> getUserAssessment(String userId, String assessmentId, Instant takenDate){
        Query query = new Query();
        query.addCriteria(where("userId").is(userId));
        query.addCriteria(where("assessmentId").is(assessmentId));
        query.addCriteria(where("takenDate").is(Date.from(takenDate)));

        Try<UserAssessment> maybeUserAssessment = hystrixCommandFactory.getMongoFindOneCommand(
                "UserAssessmentRepositoryImpl-getUserAssessment", mongoTemplate, query, UserAssessment.class).execute();

        if(maybeUserAssessment.isFailure()){
            return maybeUserAssessment;
        }

        if(!maybeUserAssessment.toOptional().isPresent()){
            return new Try.Failure<>(new RepoNotFoundException("UserAssessment"));
        }

        return maybeUserAssessment;
    }

    @Override
    public Try<UserAssessment> getUserAssessmentById(String userId, String userAssessmentId){

        Try<UserAssessment> maybeUserAssessment = hystrixCommandFactory.getMongoFindByIdCommand(
                "AssessmentRepositoryImpl-getUserAssessmentById", mongoTemplate, userAssessmentId, UserAssessment.class).execute();
        if(maybeUserAssessment.isFailure()){
            return maybeUserAssessment;
        }

        if(!maybeUserAssessment.toOptional().isPresent() || !maybeUserAssessment.get().getUserId().equals(userId)){
            return new Try.Failure<>(new RepoNotFoundException("UserAssessment"));
        }

        return maybeUserAssessment;
    }

    @Override
    public Try<UserAssessment> getLatestUserAssessment(String userId, String assessmentId){
        Query query = new Query();
        query.addCriteria(where("userId").is(userId));
        query.addCriteria(where("assessmentId").is(assessmentId));
        query.with(new Sort(new Sort.Order(Sort.Direction.DESC, "takenDate")));
        query.limit(1);

        Try<UserAssessment> maybeUserAssessment = hystrixCommandFactory.getMongoFindOneCommand(
                "UserAssessmentRepositoryImpl-getLatestUserAssessment", mongoTemplate, query, UserAssessment.class).execute();

        if(maybeUserAssessment.isFailure()){
            return maybeUserAssessment;
        }

        if(!maybeUserAssessment.toOptional().isPresent()){
            return new Try.Failure<>(new RepoNotFoundException("UserAssessment"));
        }

        return maybeUserAssessment;
    }

    @Override
    public Try<UserAssessment> getLatestUserAssessment(String userId, AssessmentCategory assessmentCategory){
        Query query = new Query();
        query.addCriteria(where("userId").is(userId));
        query.addCriteria(where("assessmentCategory").is(assessmentCategory.toString()));
        query.with(new Sort(new Sort.Order(Sort.Direction.DESC, "takenDate")));
        query.limit(1);

        Try<UserAssessment> maybeUserAssessment = hystrixCommandFactory.getMongoFindOneCommand(
                "UserAssessmentRepositoryImpl-getLatestUserAssessment", mongoTemplate, query, UserAssessment.class).execute();

        if(maybeUserAssessment.isFailure()){
            return maybeUserAssessment;
        }

        if(!maybeUserAssessment.toOptional().isPresent()){
            return new Try.Failure<>(new RepoNotFoundException("UserAssessment"));
        }

        return maybeUserAssessment;
    }

    @Override
    public Try<List<UserAssessment>> getUserAssessments(String userId, String assessmentId, Instant takenDate){
        Query query = new Query();
        query.addCriteria(where("userId").is(userId));
        query.addCriteria(where("assessmentId").is(assessmentId));

        query.addCriteria(where("takenDate").is(Date.from(takenDate)));

        return hystrixCommandFactory.getMongoFindCommand(
                "UserAssessmentRepositoryImpl-getUserAssessments", mongoTemplate, query, UserAssessment.class).execute();
    }

    @Override
    public Try<List<UserAssessment>> getUserAssessments(List<CompletionStatus> statuses, List<ScoringType> scoringTypes, String userId, Integer limit, Integer offset){
        Query query = new Query();

        if(userId != null){
            query.addCriteria(where("userId").is(userId));
        }

        query.addCriteria(where("status").in(statuses));

        if(scoringTypes != null){
            query.addCriteria(where("scoringType").in(scoringTypes));
        }

        query.with(new Sort(new Sort.Order(Sort.Direction.DESC, "takenDate")));
        if(limit != null){
            query.limit(limit);
        }

        if(offset != null){
            query.skip(offset);
        }

        return hystrixCommandFactory.getMongoFindCommand(
                "UserAssessmentRepositoryImpl-getUserAssessments", mongoTemplate, query, UserAssessment.class).execute();
    }

    @Override
    public Try<List<UserAssessment>> getCompletedUserAssessments(Instant startDate, Instant endDate){
        Query query = new Query();

        query.addCriteria(where("completionDate").gte(Date.from(startDate)).lt(Date.from(endDate)));
        query.with(new Sort(new Sort.Order(Sort.Direction.DESC, "completionDate")));

        return hystrixCommandFactory.getMongoFindCommand(
                "UserAssessmentRepositoryImpl-getCompletedUserAssessments", mongoTemplate, query, UserAssessment.class).execute();
    }

    @Override
    public Try<List<UserAssessment>> getUserAssessments(List<CompletionStatus> statuses, List<String> assessmentIds){
        Query query = new Query();
        query.addCriteria(where("assessmentId").in(assessmentIds));
        query.addCriteria(where("status").in(statuses));

        query.with(new Sort(new Sort.Order(Sort.Direction.DESC, "takenDate")));

        return hystrixCommandFactory.getMongoFindCommand(
                "UserAssessmentRepositoryImpl-getUserAssessments", mongoTemplate, query, UserAssessment.class).execute();
    }

    @Override
    public Try<List<UserAssessment>> getUserAssessments(String userId, String assessmentId){
        Query query = new Query();
        query.addCriteria(where("userId").is(userId));
        query.addCriteria(where("assessmentId").is(assessmentId));

        query.with(new Sort(new Sort.Order(Sort.Direction.DESC, "takenDate")));

        return hystrixCommandFactory.getMongoFindCommand(
                "UserAssessmentRepositoryImpl-getUserAssessments", mongoTemplate, query, UserAssessment.class).execute();
    }

    @Override
    public Try<List<UserAssessment>> getUserAssessments(String userId){
        Query query = new Query();
        query.addCriteria(where("userId").is(userId));

        query.with(new Sort(new Sort.Order(Sort.Direction.DESC, "takenDate")));

        return hystrixCommandFactory.getMongoFindCommand(
                "UserAssessmentRepositoryImpl-getUserAssessments", mongoTemplate, query, UserAssessment.class).execute();
    }

    @Override
    public Try<List<UserAssessment>> getUserAssessments(String userId, AssessmentCategory assessmentCategory, Instant takenDate){
        Query query = new Query();
        query.addCriteria(where("userId").is(userId));
        query.addCriteria(where("assessmentCategory").is(assessmentCategory.toString()));

        if(takenDate != null){
            query.addCriteria(where("takenDate").is(Date.from(takenDate)));
        }

        query.with(new Sort(new Sort.Order(Sort.Direction.DESC, "takenDate")));

        return hystrixCommandFactory.getMongoFindCommand(
                "UserAssessmentRepositoryImpl-getUserAssessments", mongoTemplate, query, UserAssessment.class).execute();
    }

    @Override
    public Try<List<UserAssessment>> getLatestUserAssessments(String userId, List<String> assessmentIds){
        Query query = new Query();
        query.addCriteria(where("userId").is(userId));
        query.addCriteria(where("assessmentId").in(assessmentIds));
        query.with(new Sort(new Sort.Order(Sort.Direction.DESC, "takenDate")));
        query.limit(assessmentIds.size());

        Try<List<UserAssessment>> maybeUserAssessments = hystrixCommandFactory.getMongoFindCommand(
                "UserAssessmentRepositoryImpl-getLatestUserAssessments", mongoTemplate, query, UserAssessment.class).execute();

        if(maybeUserAssessments.isFailure()){
            return maybeUserAssessments;
        }

        Map<String, UserAssessment> userAssessments = new HashMap<>();
        for(UserAssessment userAssessment : maybeUserAssessments.get()){
            if(userAssessments.containsKey(userAssessment.getAssessmentId())) continue;
            userAssessments.put(userAssessment.getAssessmentId(), userAssessment);
        }

        return new Try.Success<>(new ArrayList<>(userAssessments.values()));
    }

    @Override
    public Try<Map<AssessmentCategory, List<UserAssessment>>> getUserAssessmentsByCategory(String userId){
        Query query = new Query();
        query.addCriteria(where("userId").is(userId));
        query.with(new Sort(new Sort.Order(Sort.Direction.DESC, "takenDate")));

        Try<List<UserAssessment>> maybeUserAssessments = hystrixCommandFactory.getMongoFindCommand(
                "UserAssessmentRepositoryImpl-getUserAssessmentsByCategory", mongoTemplate, query, UserAssessment.class).execute();

        if(maybeUserAssessments.isFailure()){
            return new Try.Failure<>(maybeUserAssessments.failed().get());
        }

        Map<AssessmentCategory, List<UserAssessment>> userAssessments = new HashMap<>();
        for(UserAssessment userAssessment : maybeUserAssessments.get()){
            AssessmentCategory category = userAssessment.getAssessmentCategory();

            if(!userAssessments.containsKey(category)){
                userAssessments.put(category, new ArrayList<>());
            }

            userAssessments.get(category).add(userAssessment);
        }

        return new Try.Success<>(userAssessments);
    }

    @Override
    public Try<Void> saveUserAssessment(UserAssessment userAssessment) {
        return hystrixCommandFactory.getMongoSaveCommand(
                "UserAssessmentRepositoryImpl-saveUserAssessment", mongoTemplate, userAssessment).execute();
    }

    @Override
    public Try<Void> insertUserAssessment(UserAssessment userAssessment) {
        Try<UserAssessment> maybeUserAssessment = getUserAssessment(userAssessment.getUserId(), userAssessment.getAssessmentId(), userAssessment.getTakenDate());

        if(maybeUserAssessment.isFailure()){
            if(!(maybeUserAssessment.failed().get() instanceof RepoNotFoundException)){
                return new Try.Failure<>(maybeUserAssessment.failed().get());
            }
        }

        if(maybeUserAssessment.toOptional().isPresent()){
            return new Try.Failure<>(new AlreadyExistsException("UserAssessment", "username", userAssessment.getUsername()));
        }

        return hystrixCommandFactory.getMongoInsertCommand(
                "UserAssessmentRepositoryImpl-insertUserAssessment", mongoTemplate, userAssessment).execute();
    }

    @Override
    public Try<UserAssessment> getLatestUserAssessment(String userId){

        Query query = new Query();
        query.addCriteria(where("userId").is(userId));
        query.with(new Sort(new Sort.Order(Sort.Direction.DESC, "takenDate")));

        Try<UserAssessment> maybeUserAssessment = hystrixCommandFactory.getMongoFindOneCommand(
                "UserAssessmentRepositoryImpl-getLatestUserAssessment", mongoTemplate, query, UserAssessment.class).execute();

        if(maybeUserAssessment.isFailure()){
            return maybeUserAssessment;
        }

        if(!maybeUserAssessment.toOptional().isPresent()){
            return new Try.Failure<>(new RepoNotFoundException("UserAssessment"));
        }

        return maybeUserAssessment;
    }

    @Override
    public Try<List<UserAssessment>> getUserAssessments(AssessmentCategory[] assessmentCategory, CompletionStatus completionStatus, Instant startDate, Instant endDate){

        Query query = new Query();

        query.addCriteria(where("status").is(completionStatus));
        if((assessmentCategory != null) && (assessmentCategory.length > 0)) {
            query.addCriteria(where("assessmentCategory").in(assessmentCategory));
        }
        query.addCriteria(where("takenDate").gte(Date.from(startDate)).lt(Date.from(endDate)));
        query.with(new Sort(new Sort.Order(Sort.Direction.DESC, "takenDate")));

        return hystrixCommandFactory.getMongoFindCommand(
                "UserAssessmentRepositoryImpl-getGradedUserAssessments", mongoTemplate, query, UserAssessment.class).execute();

    }

}
