package com.daacs.model.assessment

import com.daacs.model.ListItemMappable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.annotations.ApiModel

import javax.validation.constraints.NotNull
/**
 * Created by chostetter on 7/27/16.
 */
@JsonIgnoreProperties(["metaClass"])
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "domainType",
        visible = true)

@JsonSubTypes([
        @JsonSubTypes.Type(value = ScoringDomain.class, name = "SCORING"),
        @JsonSubTypes.Type(value = AnalysisDomain.class, name = "ANALYSIS")
])
@ApiModel(value = "Domain", subTypes = [ScoringDomain.class, AnalysisDomain.class])
@JsonInclude(JsonInclude.Include.NON_NULL)
abstract class Domain implements ListItemMappable {
    @NotNull
    String id = (id == null) ? UUID.randomUUID().toString() : id

    String lightsideModelFilename

    @NotNull
    String label;

    @NotNull
    String content;

    @NotNull
    DomainType domainType;
}
