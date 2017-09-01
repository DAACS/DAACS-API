package com.daacs.model.assessment.user

import com.daacs.model.item.ItemGroup
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import javax.validation.Valid

/**
 * Created by chostetter on 7/5/16.
 */

@JsonIgnoreProperties(["metaClass"])
public class ItemGroupUserAssessment<T extends ItemGroup> extends UserAssessment{

    @Valid
    List<T> itemGroups = [];

}
