package com.daacs.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModel
import org.hibernate.validator.constraints.Email

import javax.validation.constraints.NotNull

/**
 * Created by chostetter on 3/2/17.
 */
@JsonIgnoreProperties(["metaClass"])
@ApiModel
class ForgotPasswordRequest {
    @NotNull
    @Email
    String username;
}
