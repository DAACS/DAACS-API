package com.daacs.model.dto.assessmentUpdate

import com.daacs.model.ListItemMappable
import com.daacs.model.item.Difficulty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import javax.validation.constraints.NotNull


/**
 * Created by mgoldman on 12/04/18.
 */
@JsonIgnoreProperties(["metaClass"])
class CATItemGroupRequest extends ItemGroupRequest implements ListItemMappable {
    @NotNull
    Difficulty difficulty;
}
