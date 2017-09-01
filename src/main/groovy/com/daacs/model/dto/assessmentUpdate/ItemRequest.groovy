package com.daacs.model.dto.assessmentUpdate

import com.daacs.model.dto.ListItemDTOMappable

import javax.validation.Valid
import javax.validation.constraints.NotNull

/**
 * Created by alandistasio on 10/20/16.
 */
class ItemRequest implements ListItemDTOMappable {

    @NotNull
    String id

    @Valid
    @NotNull
    ItemContentRequest itemContent

    @NotNull
    String question

    @Valid
    @NotNull
    List<ItemAnswerRequest> possibleItemAnswers
}
