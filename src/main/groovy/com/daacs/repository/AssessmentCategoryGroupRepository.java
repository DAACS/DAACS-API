package com.daacs.repository;

import com.daacs.model.assessment.AssessmentCategoryGroup;
import com.lambdista.util.Try;

import java.util.List;

/**
 * Created by mgoldman on 2/28/19.
 */
public interface AssessmentCategoryGroupRepository {

    Try<List<AssessmentCategoryGroup>> getCategoryGroups();

    Try<Void> createCategoryGroup(AssessmentCategoryGroup categoryGroup);

    Try<Void> updateCategoryGroup(AssessmentCategoryGroup categoryGroup);

    Try<Void> deleteCategoryGroup(String id);

    Try<AssessmentCategoryGroup> getCategoryGroupById(String id);

    Try<AssessmentCategoryGroup> getCategoryGroupByIdOrNull(String id);

    Try<List<AssessmentCategoryGroup>> getGlobalGroups();
}
