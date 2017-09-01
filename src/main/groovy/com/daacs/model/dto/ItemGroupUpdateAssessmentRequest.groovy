package com.daacs.model.dto

import com.daacs.model.dto.assessmentUpdate.ItemGroupRequest

import javax.validation.constraints.NotNull

/**
 * Created by alandistasio on 10/25/16.
 */
class ItemGroupUpdateAssessmentRequest extends UpdateAssessmentRequest {

    @NotNull
    List<ItemGroupRequest> itemGroups;
}
