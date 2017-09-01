package com.daacs.service;

import com.daacs.model.assessment.*;
import com.daacs.model.dto.UpdateAssessmentRequest;
import com.daacs.model.dto.UpdateLightSideModelsRequest;
import com.lambdista.util.Try;

import java.time.Instant;
import java.util.List;

/**
 * Created by chostetter on 7/7/16.
 */
public interface AssessmentService {
    Try<Assessment> getAssessment(String id);
    Try<List<Assessment>> getAssessments(Boolean enabled, List<AssessmentCategory> assessmentCategories);
    Try<Assessment> createAssessment(Assessment assessment);
    Try<Assessment> updateAssessment(UpdateAssessmentRequest updateAssessmentRequest);
    Try<List<AssessmentSummary>> getSummaries(String userId, Boolean enabled, List<AssessmentCategory> assessmentCategories);
    Try<Assessment> saveAssessment(Assessment assessment);
    Try<Void> reloadDummyAssessments();
    Try<AssessmentContent> getContent(String assessmentId);
    Try<AssessmentContent> getContent(AssessmentCategory assessmentCategory);
    Try<AssessmentContent> getContentForUserAssessment(String userId, AssessmentCategory assessmentCategory, Instant takenDate);
    Try<Assessment> updateWritingAssessment(UpdateLightSideModelsRequest updateLightSideModelsRequest);
    Try<List<AssessmentStatSummary>> getAssessmentStats();
    Try<List<AssessmentCategorySummary>> getCategorySummaries(String userId, List<AssessmentCategory> assessmentCategories);
}
