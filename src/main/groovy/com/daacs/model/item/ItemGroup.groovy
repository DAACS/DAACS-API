package com.daacs.model.item

import com.daacs.framework.serializer.Views
import com.daacs.model.ListItemMappable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonView

import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

/**
 * Created by chostetter on 7/6/16.
 */

@JsonIgnoreProperties(["metaClass"])
class ItemGroup implements ListItemMappable {
    @NotNull
    @JsonView([Views.NotExport])
    String id = UUID.randomUUID().toString() //default to new ID

    @NotNull
    @Valid
    List<DefaultItemAnswer> possibleItemAnswers;

    @NotNull
    @Size(min = 1)
    @Valid
    List<Item> items;
}
