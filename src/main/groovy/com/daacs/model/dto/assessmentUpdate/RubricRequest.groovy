package com.daacs.model.dto.assessmentUpdate

import javax.validation.Valid
import javax.validation.constraints.NotNull

/**
 * Created by alandistasio on 10/20/16.
 */
class RubricRequest {
    @Valid
    @NotNull
    List<SupplementTableRowRequest> supplementTable;
}
