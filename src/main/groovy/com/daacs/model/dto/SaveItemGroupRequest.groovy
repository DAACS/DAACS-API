package com.daacs.model.dto

import com.daacs.model.item.ItemGroup

import javax.validation.constraints.NotNull

/**
 * Created by chostetter on 7/25/16.
 */
class SaveItemGroupRequest extends ItemGroup {

    @NotNull
    String assessmentId;

}
