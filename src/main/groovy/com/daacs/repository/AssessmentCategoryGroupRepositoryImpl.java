package com.daacs.repository;

import com.daacs.component.HystrixCommandFactory;
import com.daacs.component.utils.CategoryGroupUtils;
import com.daacs.framework.exception.RepoNotFoundException;
import com.daacs.model.assessment.AssessmentCategoryGroup;
import com.lambdista.util.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Created by mgoldman on 2/28/19.
 */
@Repository
public class AssessmentCategoryGroupRepositoryImpl implements AssessmentCategoryGroupRepository {

    @Autowired
    private HystrixCommandFactory hystrixCommandFactory;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CategoryGroupUtils categoryGroupUtils;

    @Override
    public Try<List<AssessmentCategoryGroup>> getGlobalGroups() {
        Criteria valueEmpty = Criteria.where("samlValue").is("");
        Criteria valueNull = Criteria.where("samlValue").is(null);

        Criteria fieldEmpty = Criteria.where("samlField").is("");
        Criteria fieldNull = Criteria.where("samlField").is(null);

        Criteria orNoValue = new Criteria().orOperator(valueEmpty, valueNull );
        Criteria orNoField = new Criteria().orOperator(fieldEmpty, fieldNull );

        Criteria ignoreDefaultGroups = new Criteria().where("_id").nin(categoryGroupUtils.getDefaultIds());

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").exists(true));
        query.addCriteria(new Criteria().andOperator(orNoValue,orNoField,ignoreDefaultGroups));

        return hystrixCommandFactory.getMongoFindCommand(
                "AssessmentCategoryGroupRepositoryImpl-getCategoryGroups", mongoTemplate, query, AssessmentCategoryGroup.class).execute();
    }

    @Override
    public Try<List<AssessmentCategoryGroup>> getCategoryGroups() {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").exists(true));

        return hystrixCommandFactory.getMongoFindCommand(
                "AssessmentCategoryGroupRepositoryImpl-getCategoryGroups", mongoTemplate, query, AssessmentCategoryGroup.class).execute();
    }

    @Override
    public Try<Void> createCategoryGroup(AssessmentCategoryGroup categoryGroup) {
        return hystrixCommandFactory.getMongoInsertCommand(
                "AssessmentCategoryGroupRepositoryImpl-createCategoryGroup", mongoTemplate, categoryGroup).execute();
    }

    @Override
    public Try<Void> updateCategoryGroup(AssessmentCategoryGroup categoryGroup) {
        return hystrixCommandFactory.getMongoSaveCommand(
                "AssessmentCategoryGroupRepositoryImpl-updateCategoryGroup", mongoTemplate, categoryGroup).execute();

    }

    @Override
    public Try<Void> deleteCategoryGroup(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(id));

        return hystrixCommandFactory.getMongoDeleteByIdCommand(
                "AssessmentCategoryGroupRepositoryImpl-deleteCategoryGroup", mongoTemplate, query, AssessmentCategoryGroup.class).execute();
    }

    @Override
    public Try<AssessmentCategoryGroup> getCategoryGroupById(String id) {

        Try<AssessmentCategoryGroup> maybeGroup = hystrixCommandFactory.getMongoFindByIdCommand(
                "AssessmentCategoryGroupRepositoryImpl-getCategoryGroupById", mongoTemplate, id, AssessmentCategoryGroup.class).execute();

        if (maybeGroup.isFailure()) {
            return maybeGroup;
        }

        if (!maybeGroup.toOptional().isPresent()) {
            return new Try.Failure<>(new RepoNotFoundException("AssessmentCategoryGroup"));
        }

        return maybeGroup;
    }


    //only used by UpgradeAssessmentSchemaUtils to create default groups during backfill
    @Override
    public Try<AssessmentCategoryGroup> getCategoryGroupByIdOrNull(String id) {

        Try<AssessmentCategoryGroup> maybeGroup = hystrixCommandFactory.getMongoFindByIdCommand(
                "AssessmentCategoryGroupRepositoryImpl-getCategoryGroupByIdOrNull", mongoTemplate, id, AssessmentCategoryGroup.class).execute();

        if (maybeGroup.isFailure()) {
            return maybeGroup;
        }

        if (!maybeGroup.toOptional().isPresent()) {
            return new Try.Success<>(null);
        }

        return maybeGroup;
    }
}
