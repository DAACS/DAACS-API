package com.daacs.repository;

import com.daacs.component.HystrixCommandFactory;
import com.daacs.framework.exception.RepoNotFoundException;
import com.daacs.model.InstructorClass;
import com.lambdista.util.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;


@Repository
public class InstructorClassRepositoryImpl implements InstructorClassRepository {

    @Autowired
    private HystrixCommandFactory hystrixCommandFactory;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Try<InstructorClass> getClass(String id) {
        Try<InstructorClass> maybeClass = hystrixCommandFactory.getMongoFindByIdCommand(
                "ClassRepositoryImpl-getClass", mongoTemplate, id, InstructorClass.class).execute();

        if(maybeClass.isFailure()){
            return maybeClass;
        }

        if(!maybeClass.toOptional().isPresent()){
            return new Try.Failure<>(new RepoNotFoundException("InstructorClass"));
        }

        return maybeClass;
    }

    @Override
    public Try<InstructorClass> getClassByNameAndInstructor(String instructorId, String className) {
        Query query = new Query();
        query.addCriteria(Criteria.where("name").is(className));
        query.addCriteria(Criteria.where("instructorId").is(instructorId));

        Try<InstructorClass> maybeClass = hystrixCommandFactory.getMongoFindOneCommand("ClassRepositoryImpl-getClasses", mongoTemplate, query, InstructorClass.class).execute();

        if(maybeClass.isFailure()){
            return maybeClass;
        }

        //returns null if class doesn't exist
        if(!maybeClass.toOptional().isPresent()){
            return new Try.Success<>(null);
        }

        return maybeClass;
    }

    @Override
    public Try<Void> saveClass(InstructorClass instructorClass) {
        return hystrixCommandFactory.getMongoSaveCommand("ClassRepositoryImpl-saveClass", mongoTemplate, instructorClass).execute();
    }

    @Override
    public synchronized Try<Void> insertClass(InstructorClass instructorClass) {
        return hystrixCommandFactory.getMongoInsertCommand("ClassRepositoryImpl-insertClass", mongoTemplate, instructorClass).execute();
    }


    @Override
    public Try<List<InstructorClass>> getClasses(String[] instructorIds, Integer limit, Integer offset){
        Query query = new Query();
        if(instructorIds.length > 0){
            query.addCriteria(Criteria.where("instructorId").in(Arrays.asList(instructorIds)));
        }
        query.skip(offset).limit(limit);

        return hystrixCommandFactory.getMongoFindCommand("ClassRepositoryImpl-getClasses", mongoTemplate, query, InstructorClass.class).execute();
    }

    @Override
    public Try<List<InstructorClass>> getClassByStudentAndAssessmentId(String studentId, String assessmentId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("studentInvites.studentId").is(studentId));
        query.addCriteria(Criteria.where("assessmentIds").is(assessmentId));

        return hystrixCommandFactory.getMongoFindCommand("ClassRepositoryImpl-getClassByStudentAndAssessmentId", mongoTemplate, query, InstructorClass.class).execute();
    }
}
