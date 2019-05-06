package com.daacs.model.assessment

import org.springframework.data.annotation.Id

import javax.validation.constraints.NotNull

/**
 * Created by mgoldman on 2/28/19.
 */
class AssessmentCategoryGroup {

    @Id
    @NotNull
    String id
    @NotNull
    String label
    @NotNull
    AssessmentCategory assessmentCategory
    String samlValue
    String samlField

}
