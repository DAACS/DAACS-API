package com.daacs.service;

import com.daacs.model.assessment.CATAssessment;
import com.daacs.model.assessment.MultipleChoiceAssessment;
import com.daacs.model.assessment.user.CATUserAssessment;
import com.daacs.model.assessment.user.MultipleChoiceUserAssessment;
import com.daacs.model.item.ItemGroup;
import com.lambdista.util.Try;

/**
 * Created by chostetter on 7/21/16.
 */
public interface ItemGroupService {
    Try<ItemGroup> getNextItemGroup(String userId, String assessmentId);
    Try<ItemGroup> getNextCATItemGroup(String userId, CATUserAssessment userAssessment, CATAssessment assessment);
    Try<ItemGroup> getNextMultipleChoiceItemGroup(String userId, MultipleChoiceUserAssessment userAssessment, MultipleChoiceAssessment assessment);
    Try<ItemGroup> saveItemGroup(String userId, String assessmentId, ItemGroup itemGroup);
}
