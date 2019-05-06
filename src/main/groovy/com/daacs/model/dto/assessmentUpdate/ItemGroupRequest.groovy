package com.daacs.model.dto.assessmentUpdate

import com.daacs.model.dto.ListItemDTOMappable
import com.daacs.model.item.DefaultItemAnswer

import javax.validation.Valid
import javax.validation.constraints.NotNull


/**
 * Created by alandistasio on 10/20/16.
 */
class ItemGroupRequest implements ListItemDTOMappable {

    @NotNull
    String id = (id == null) ? UUID.randomUUID().toString() : id;

    @NotNull
    @Valid
    List<DefaultItemAnswer> possibleItemAnswers;

    @Valid
    @NotNull
    List<ItemRequest> items;
}
