package com.daacs.repository;

import com.daacs.component.HystrixCommandFactory;
import com.daacs.framework.exception.RepoNotFoundException;
import com.daacs.model.assessment.Assessment;
import com.daacs.model.assessment.AssessmentCategory;
import com.daacs.model.assessment.ScoringType;
import com.daacs.model.assessment.user.UserAssessment;
import com.lambdista.util.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;

/**
 * Created by chostetter on 7/7/16.
 */

@Repository
public class AssessmentRepositoryImpl implements AssessmentRepository {

    @Autowired
    private HystrixCommandFactory hystrixCommandFactory;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Try<Assessment> getAssessment(String id) {

        Try<Assessment> maybeAssessment = hystrixCommandFactory.getMongoFindByIdCommand(
                "AssessmentRepositoryImpl-getAssessment", mongoTemplate, id, Assessment.class).execute();

        if(maybeAssessment.isFailure()){
            return maybeAssessment;
        }

        if(!maybeAssessment.toOptional().isPresent()){
            return new Try.Failure<>(new RepoNotFoundException("Assessment"));
        }

        return maybeAssessment;
    }

    @Override
    public Try<Void> insertAssessment(Assessment assessment) {

        return hystrixCommandFactory.getMongoInsertCommand(
                "AssessmentRepositoryImpl-insertAssessment", mongoTemplate, assessment).execute();
    }

    @Override
    public Try<Void> saveAssessment(Assessment assessment) {

        return hystrixCommandFactory.getMongoSaveCommand(
                "AssessmentRepositoryImpl-saveAssessment", mongoTemplate, assessment).execute();
    }

    @Override
    public Try<List<Assessment>> getAssessments(Boolean enabled, List<AssessmentCategory> assessmentCategories) {
        Query query = new Query();
        if(enabled != null){
            query.addCriteria(Criteria.where("enabled").is(enabled));
        }

        query.addCriteria(Criteria.where("assessmentCategory").in(assessmentCategories));

        return hystrixCommandFactory.getMongoFindCommand(
                "AssessmentRepositoryImpl-getAssessments", mongoTemplate, query, Assessment.class).execute();
    }

    @Override
    public Try<List<Assessment>> getAssessments(List<ScoringType> scoringTypes, Boolean enabled) {
        Query query = new Query();

        if(enabled != null){
            query.addCriteria(Criteria.where("enabled").is(enabled));
        }

        query.addCriteria(Criteria.where("scoringType").in(scoringTypes));

        return hystrixCommandFactory.getMongoFindCommand(
                "AssessmentRepositoryImpl-getAssessments", mongoTemplate, query, Assessment.class).execute();
    }

    @Override
    public Try<List<Map>> getAssessmentStats() {
        Aggregation agg = Aggregation.newAggregation(
                group("assessmentId", "status", "assessmentCategory").count().as("total")
        );

        return hystrixCommandFactory.getMongoAggregateCommand("AssessmentRepositoryImpl-getAssessmentStats", mongoTemplate, agg, UserAssessment.class, Map.class).execute();
    }
}
