package com.daacs.model.item

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.common.collect.Range

import javax.validation.constraints.NotNull

/**
 * Created by chostetter on 7/6/16.
 */

@JsonIgnoreProperties(["metaClass"])
class ItemGroupTransition {
    @NotNull
    Difficulty groupDifficulty;

    @NotNull
    Map<Difficulty, Range<Double>> transitionMap;
}
