package com.daacs.model.item

import com.daacs.framework.serializer.Views
import com.daacs.framework.validation.annotations.group.CreateGroup
import com.daacs.model.ListItemMappable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonView

import javax.validation.constraints.NotNull
/**
 * Created by chostetter on 7/6/16.
 */

@JsonIgnoreProperties(["metaClass"])
class ItemAnswer implements ListItemMappable {
    @NotNull
    @JsonView([Views.NotExport])
    String id = UUID.randomUUID().toString() //default to new ID

    @NotNull
    String content;

    @JsonView([Views.CompletedAssessment, Views.Admin, Views.Export])
    @NotNull(groups = [ CreateGroup.class ])
    Integer score;
}
