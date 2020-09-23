package com.daacs.model.dto

import com.daacs.model.User
import org.hibernate.validator.constraints.NotBlank
import org.springframework.data.annotation.Id

import javax.validation.constraints.NotNull

/**
 * Created by mgoldman
 */

class InstructorClassUserScore {
    InstructorClassUserScore(User user) {
        this.id = user.getId()
        this.studentEmail =  user.getUsername()
        this.studentFirstName = user.getFirstName()
        this.studentLastName = user.getLastName()
    }

    @Id
    String id

    @NotNull
    Boolean classInviteAccepted

    @NotBlank
    String studentEmail

    @NotBlank
    String studentFirstName

    @NotBlank
    String studentLastName

    @NotNull
    List<AssessmentScore> assessmentScores = new ArrayList<>()
}

class AssessmentScore{
    AssessmentScore(String assessmentId, String assessmentCategoryGroup, String overallScore) {
        this.assessmentId = assessmentId
        this.assessmentCategoryGroup = assessmentCategoryGroup
        this.overallScore = overallScore
    }

    String assessmentId
    String assessmentCategoryGroup
    String overallScore
}

