package com.daacs.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModel

import javax.validation.constraints.NotNull
/**
 * Created by chostetter on 3/2/17.
 */
@JsonIgnoreProperties(["metaClass"])
@ApiModel
class ResetPasswordRequest {
    @NotNull
    String userId;

    @NotNull
    String password;

    @NotNull
    String passwordConfirm;

    @NotNull
    String code;
}
