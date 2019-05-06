package com.daacs.service;

import com.daacs.model.assessment.*;
import com.daacs.model.dto.AssessmentResponse;
import com.daacs.model.dto.UpdateAssessmentRequest;
import com.lambdista.util.Try;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

/**
 * Created by chostetter on 7/7/16.
 */
public interface AssessmentService {
    Try<Assessment> getAssessment(String id);
    Try<List<Assessment>> getAssessments(Boolean enabled, List<String> groupIds);
    Try<AssessmentResponse> createAssessment(Assessment assessment);
    Try<AssessmentResponse> updateAssessment(UpdateAssessmentRequest updateAssessmentRequest);

    Try<List<AssessmentSummary>> getSummaries(String userId, Boolean enabled, List<String> groupIds);

    Try<Assessment> saveAssessment(Assessment assessment);
    Try<Void> reloadDummyAssessments();
    Try<AssessmentContent> getContent(String assessmentId);

    Try<AssessmentContent> getContentByCategoryGroup(String groupId);

    Try<AssessmentContent> getContentForUserAssessment(String userId, String groupId, Instant takenDate);

    Try<String> uploadLightSideModel(MultipartFile file);
    Try<List<AssessmentStatSummary>> getAssessmentStats();
    Try<List<AssessmentCategorySummary>> getCategorySummaries(String userId, List<String> groupIds);
}
