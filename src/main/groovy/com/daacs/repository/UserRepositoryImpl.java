package com.daacs.repository;

import com.daacs.component.HystrixCommandFactory;
import com.daacs.framework.exception.AlreadyExistsException;
import com.daacs.model.User;
import com.daacs.model.UserSearchResult;
import com.lambdista.util.Try;
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
public class UserRepositoryImpl implements UserRepository {

    @Autowired
    private HystrixCommandFactory hystrixCommandFactory;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Try<User> getUser(String id) {
        return hystrixCommandFactory.getMongoFindByIdCommand("UserRepositoryImpl-getUser", mongoTemplate, id, User.class).execute();
    }

    @Override
    public Try<User> getUserByUsername(String username) {
        Query query = new Query();
        query.addCriteria(Criteria.where("username").is(username));

        return hystrixCommandFactory.getMongoFindOneCommand("UserRepositoryImpl-getUserByUsername", mongoTemplate, query, User.class).execute();
    }

    @Override
    public Try<User> getUserBySecondaryId(String secondaryId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("secondaryId").is(secondaryId));

        return hystrixCommandFactory.getMongoFindOneCommand("UserRepositoryImpl-getUserBySecondaryId", mongoTemplate, query, User.class).execute();
    }

    @Override
    public Try<Void> saveUser(User user) {
        return hystrixCommandFactory.getMongoSaveCommand("UserRepositoryImpl-saveUser", mongoTemplate, user).execute();
    }

    @Override
    public synchronized Try<Void> insertUser(User user) {
        Try<User> maybeUser = getUserByUsername(user.getUsername());

        if(maybeUser.isFailure()) {
            return new Try.Failure<>(maybeUser.failed().get());
        }

        if(maybeUser.toOptional().isPresent()){
            return new Try.Failure<>(new AlreadyExistsException("User", "username", user.getUsername()));
        }

        return hystrixCommandFactory.getMongoInsertCommand("UserRepositoryImpl-insertAssessment", mongoTemplate, user).execute();
    }

    @Override
    public Try<List<UserSearchResult>> searchUsers(List<String> keywords, int limit){

        List<AggregationOperation> aggregationOperations = new ArrayList<>();

        aggregationOperations.add(group(Fields.from(Fields.field("firstName"), Fields.field("lastName"), Fields.field("username"), Fields.field("userId", "$id"))));

        aggregationOperations.add(project(Fields.from(Fields.field("firstName"), Fields.field("lastName"), Fields.field("username"), Fields.field("_id", "$_id.userId")))
                .andExpression("concat(firstName, lastName, username)")
                .as("searchField"));

        aggregationOperations.addAll(
                keywords.stream()
                        .map(keyword -> match(Criteria.where("searchField")
                                .regex(keyword.replaceAll("/[#-}]/g", "\\$&"), "i"))) //sanitize
                        .collect(Collectors.toList()));

        aggregationOperations.add(sort(Sort.Direction.ASC, "firstName"));
        aggregationOperations.add(limit(limit));

        Aggregation agg = newAggregation(aggregationOperations);

        return hystrixCommandFactory.getMongoAggregateCommand(
                "UserRepositoryImpl-searchUserAssessments", mongoTemplate, agg, User.class, UserSearchResult.class).execute();
    }

    @Override
    public Try<List<User>> getUsers(List<String> roles){
        Query query = new Query();
        query.addCriteria(Criteria.where("roles").in(roles));

        return hystrixCommandFactory.getMongoFindCommand("UserRepositoryImpl-getUsers", mongoTemplate, query, User.class).execute();
    }
}
