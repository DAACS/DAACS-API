package com.daacs.model.dto.assessmentUpdate

import com.daacs.model.assessment.DomainType
import com.daacs.model.dto.ListItemDTOMappable
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

import javax.validation.constraints.NotNull
/**
 * Created by alandistasio on 10/20/16.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "domainType",
        visible = true)

@JsonSubTypes([
        @JsonSubTypes.Type(value = ScoringDomainRequest.class, name = "SCORING"),
        @JsonSubTypes.Type(value = AnalysisDomainRequest.class, name = "ANALYSIS")
])
abstract class DomainRequest implements ListItemDTOMappable {
    @NotNull
    String id = (id == null) ? UUID.randomUUID().toString() : id

    @NotNull
    String label;

    @NotNull
    String content;

    @NotNull
    DomainType domainType;

    String lightsideModelFilename

}
