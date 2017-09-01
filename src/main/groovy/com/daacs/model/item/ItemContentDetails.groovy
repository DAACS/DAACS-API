package com.daacs.model.item

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import javax.validation.constraints.NotNull

/**
 * Created by alandistasio on 10/4/16.
 */
@JsonIgnoreProperties(["metaClass"])
class ItemContentDetails {
    @NotNull
    ItemContentType itemContentType

    @NotNull
    String content
}
