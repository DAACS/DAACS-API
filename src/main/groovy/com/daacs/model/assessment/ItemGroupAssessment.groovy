package com.daacs.model.assessment

import com.daacs.model.item.ItemGroup
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

/**
 * Created by chostetter on 7/6/16.
 */

@JsonIgnoreProperties(["metaClass"])
class ItemGroupAssessment<T extends ItemGroup> extends Assessment {

    @NotNull
    @Size(min = 1)
    @Valid
    List<T> itemGroups;

}
