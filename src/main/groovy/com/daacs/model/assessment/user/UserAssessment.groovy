package com.daacs.model.assessment.user

import com.daacs.model.assessment.AssessmentCategory
import com.daacs.model.assessment.AssessmentType
import com.daacs.model.assessment.ScoringType
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.format.annotation.DateTimeFormat

import java.time.Instant
/**
 * Created by chostetter on 7/5/16.
 */
@JsonIgnoreProperties(["metaClass"])
@ApiModel(value = "UserAssessment", subTypes = [CATUserAssessment.class, MultipleChoiceUserAssessment.class, WritingPromptUserAssessment.class])
@CompoundIndexes([
        @CompoundIndex(name = "userId_assessmentId_takenDate", def = "{'userId' : 1, 'assessmentId': 1, 'takenDate': 1}")
])
@Document(collection = "user_assessments")
public abstract class UserAssessment {
    @Id
    String id = UUID.randomUUID().toString();

    @Indexed
    String userId;

    @Indexed
    String username;

    @Indexed
    String firstName;

    @Indexed
    String lastName;
    
    @Indexed
    String assessmentId;

    AssessmentType assessmentType;

    AssessmentCategory assessmentCategory;

    @JsonIgnore
    @Indexed
    String assessmentCategoryGroupId

    String assessmentLabel;

    @ApiModelProperty(dataType = "java.lang.String")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant takenDate = Instant.now();

    CompletionStatus status = CompletionStatus.IN_PROGRESS;

    Double progressPercentage = 0.0;

    @Indexed
    @ApiModelProperty(dataType = "java.lang.String")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant completionDate;

    List<DomainScore> domainScores = [];

    CompletionScore overallScore;

    String gradingError;

    ScoringType scoringType;


    @Override
    public String toString() {
        return "UserAssessment{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", assessmentId='" + assessmentId + '\'' +
                ", assessmentType=" + assessmentType +
                ", assessmentCategory=" + assessmentCategory +
                ", assessmentLabel='" + assessmentLabel + '\'' +
                ", takenDate=" + takenDate +
                ", status=" + status +
                ", progressPercentage=" + progressPercentage +
                ", completionDate=" + completionDate +
                ", domainScores=" + domainScores.toString() +
                ", overallScore=" + overallScore +
                ", gradingError='" + gradingError + '\'' +
                ", scoringType=" + scoringType +
                '}';
    }
}
