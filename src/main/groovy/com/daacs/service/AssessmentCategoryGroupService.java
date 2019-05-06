package com.daacs.service;

import com.daacs.model.assessment.*;
import com.lambdista.util.Try;

import java.util.List;

/**
 * Created by mgoldman on 2/28/19.
 */
public interface AssessmentCategoryGroupService {
    Try<List<AssessmentCategoryGroup>> getCategoryGroups();

    Try<AssessmentCategoryGroup> createCategoryGroup(AssessmentCategoryGroup categoryGroup);

    Try<AssessmentCategoryGroup> updateCategoryGroup(String id, AssessmentCategoryGroup categoryGroup);

    Try<Void> deleteCategoryGroup(String id);

    Try<AssessmentCategoryGroup> createCategoryGroupIfPossible(AssessmentCategoryGroup categoryGroup);

    Try<List<String>> getGlobalGroupIds();

}
