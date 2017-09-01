package com.daacs.model.dto

import javax.validation.constraints.NotNull
/**
 * Created by chostetter on 12/15/16.
 */
class UpdateUserRequest {

    @NotNull
    String id;

    Boolean hasDataUsageConsent;

}

