package com.daacs.model.assessment


import com.daacs.framework.validation.annotations.ValidCATAssessment
import com.daacs.framework.validation.annotations.group.CreateGroup
import com.daacs.framework.validation.annotations.group.SecondaryCreateGroup
import com.daacs.model.item.CATItemGroup
import com.daacs.model.item.Difficulty
import com.daacs.model.item.ItemGroupTransition
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import javax.validation.GroupSequence
import javax.validation.Valid
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull
/**
 * Created by chostetter on 7/6/16.
 */

@JsonIgnoreProperties(["metaClass"])
@GroupSequence([CATAssessment.class, CreateGroup.class, SecondaryCreateGroup.class])
@ValidCATAssessment(groups = SecondaryCreateGroup.class)
class CATAssessment extends ItemGroupAssessment<CATItemGroup> {
    @NotNull
    Difficulty startingDifficulty;

    @NotNull
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
