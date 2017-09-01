package com.daacs.model.dto.assessmentUpdate

import com.daacs.model.dto.ListItemDTOMappable

import javax.validation.constraints.NotNull

/**
 * Created by alandistasio on 10/20/16.
 */
class ItemAnswerRequest implements ListItemDTOMappable {

    @NotNull
    String id

    @NotNull
    String content;

    @NotNull
    Integer score;
}
