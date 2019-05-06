package com.daacs.model.dto

import com.daacs.model.dto.assessmentUpdate.ItemGroupRequest
import com.daacs.model.item.Difficulty

import javax.validation.constraints.NotNull

/**
 * Created by alandistasio on 10/25/16.
 */
class ItemGroupUpdateAssessmentRequest<T extends ItemGroupRequest> extends UpdateAssessmentRequest {

    @NotNull
    Difficulty startingDifficulty;

    @NotNull
    List<T> itemGroups;
}
