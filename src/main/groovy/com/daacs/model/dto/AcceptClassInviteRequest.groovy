package com.daacs.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModel

import javax.validation.constraints.NotNull
/**
 * Created by mgoldman
 */
@JsonIgnoreProperties(["metaClass"])
@ApiModel
class AcceptClassInviteRequest {
    @NotNull
    String userId;

    @NotNull
    String classId;
}
