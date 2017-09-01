package com.daacs.model.item

import com.daacs.model.ListItemMappable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import javax.validation.constraints.NotNull

/**
 * Created by chostetter on 7/7/16.
 */

@JsonIgnoreProperties(["metaClass"])
class CATItemGroup extends ItemGroup implements ListItemMappable {
    @NotNull
    Difficulty difficulty;
}
