package com.daacs.model.assessment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import javax.validation.constraints.NotNull

/**
 * Created by chostetter on 8/16/16.
 */
@JsonIgnoreProperties(["metaClass"])
public class LightSideConfig {

    @NotNull
    Map<String, String> domainModels = [:];
}
