package com.daacs.model.dto.assessmentUpdate

import com.daacs.model.dto.ListItemDTOMappable

import javax.validation.Valid
import javax.validation.constraints.NotNull


/**
 * Created by alandistasio on 10/20/16.
 */
class ItemGroupRequest implements ListItemDTOMappable {

    @NotNull
    String id;

    @Valid
    @NotNull
    List<ItemRequest> items;
}
