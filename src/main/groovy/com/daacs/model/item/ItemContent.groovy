package com.daacs.model.item

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import javax.validation.constraints.NotNull
/**
 * Created by chostetter on 7/6/16.
 */

@JsonIgnoreProperties(["metaClass"])
class ItemContent {
    @NotNull
    ItemContentDetails question

    @NotNull
    ItemContentDetails feedback
}
