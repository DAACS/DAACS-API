package com.daacs.model.prereqs

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

import javax.validation.constraints.NotNull
/**
 * Created by chostetter on 7/5/16.
 */

@JsonIgnoreProperties(["metaClass"])

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "prereqType",
        visible = true)

@JsonSubTypes([
    @JsonSubTypes.Type(value = AssessmentPrereq.class, name = "ASSESSMENT")
])
public abstract class Prerequisite {

    @NotNull
    PrereqType prereqType;

    String reason;
}

