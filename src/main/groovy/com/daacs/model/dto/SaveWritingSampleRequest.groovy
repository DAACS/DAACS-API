package com.daacs.model.dto

import io.swagger.annotations.ApiModelProperty

import javax.validation.constraints.NotNull
import java.time.Instant
/**
 * Created by chostetter on 7/25/16.
 */
public class SaveWritingSampleRequest {

    @NotNull
    String id;

    @NotNull
    String assessmentId;

    @NotNull
    String sample;

    @NotNull
    @ApiModelProperty(dataType = "java.lang.String")
    Instant startDate;

    @ApiModelProperty(dataType = "java.lang.String")
    Instant completeDate;

}
