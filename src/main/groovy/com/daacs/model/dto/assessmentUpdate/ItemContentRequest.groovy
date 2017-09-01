package com.daacs.model.dto.assessmentUpdate

import javax.validation.constraints.NotNull

/**
 * Created by alandistasio on 10/20/16.
 */
class ItemContentRequest {
    @NotNull
    ItemContentDetailsRequest question

    @NotNull
    ItemContentDetailsRequest feedback
}
