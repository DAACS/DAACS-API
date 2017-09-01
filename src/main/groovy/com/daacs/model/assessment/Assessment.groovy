package com.daacs.model.assessment

import com.daacs.framework.serializer.Views
import com.daacs.framework.validation.annotations.ValidBaseAssessment
import com.daacs.framework.validation.annotations.group.CreateGroup
import com.daacs.framework.validation.annotations.group.SecondaryCreateGroup
import com.daacs.model.prereqs.Prerequisite
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonView
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.hibernate.validator.constraints.NotEmpty
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.format.annotation.DateTimeFormat

import javax.validation.GroupSequence
import javax.validation.Valid
import javax.validation.constraints.NotNull
import java.time.Instant
/**
 * Created by chostetter on 7/5/16.
 */

@JsonIgnoreProperties(["metaClass"])
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "assessmentType",
        visible = true)

@JsonSubTypes([
        @JsonSubTypes.Type(value = CATAssessment.class, name = "CAT"),
        @JsonSubTypes.Type(value = MultipleChoiceAssessment.class, name = "MULTIPLE_CHOICE"),
        @JsonSubTypes.Type(value = MultipleChoiceAssessment.class, name = "LIKERT"),
        @JsonSubTypes.Type(value = WritingAssessment.class, name = "WRITING_PROMPT")
])

@ApiModel(value = "Assessment", subTypes = [CATAssessment.class, MultipleChoiceAssessment.class, WritingAssessment.class])
@Document(collection = "assessments")
@GroupSequence([Assessment.class, CreateGroup.class, SecondaryCreateGroup.class])
@ValidBaseAssessment(groups = SecondaryCreateGroup.class)
abstract class Assessment {
    @Id
    @JsonView([Views.NotExport])
    String id = UUID.randomUUID().toString();

    @NotNull
    AssessmentType assessmentType;

    @NotNull
    AssessmentCategory assessmentCategory;

    @NotEmpty
    String label;

    @ApiModelProperty(dataType = "java.lang.String")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonView([Views.NotExport])
    Instant createdDate = Instant.now();

    Map<String, String> content;

    @NotNull
    @Indexed
    Boolean enabled;

    @NotNull
    @Valid
    List<Prerequisite> prerequisites;

    @NotNull
    @Valid
    Rubric overallRubric;

    @NotNull
    @Valid
    List<Domain> domains;

    @NotNull
    ScoringType scoringType;
}
