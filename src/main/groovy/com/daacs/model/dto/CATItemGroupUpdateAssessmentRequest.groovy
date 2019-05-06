package com.daacs.model.dto

import com.daacs.framework.validation.annotations.ValidCATAssessment
import com.daacs.framework.validation.annotations.group.CreateGroup
import com.daacs.framework.validation.annotations.group.SecondaryCreateGroup
import com.daacs.model.dto.assessmentUpdate.CATItemGroupRequest
import com.daacs.model.item.Difficulty
import com.daacs.model.item.ItemGroupTransition
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import javax.validation.GroupSequence
import javax.validation.Valid
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

/**
 * Created by mgoldman on 12/04/18.
 */
@JsonIgnoreProperties(["metaClass"])
@GroupSequence([CATItemGroupUpdateAssessmentRequest.class, CreateGroup.class, SecondaryCreateGroup.class])
@ValidCATAssessment(groups = SecondaryCreateGroup.class)
class CATItemGroupUpdateAssessmentRequest  extends ItemGroupUpdateAssessmentRequest<CATItemGroupRequest> {

    @NotNull
    Difficulty startingDifficulty;

    @NotNull
    List<CATItemGroupRequest> itemGroups;

    @Valid
    List<ItemGroupTransition> itemGroupTransitions;

    @NotNull
    @Min(1L)
    Integer minTakenGroups;

    @NotNull
    @Min(1L)
    Integer maxTakenGroups;

    @NotNull
    @Min(1L)
    Integer numQuestionsPerGroup;

}
