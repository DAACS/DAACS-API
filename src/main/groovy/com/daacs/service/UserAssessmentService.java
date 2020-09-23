package com.daacs.service;

import com.daacs.model.User;
import com.daacs.model.assessment.AssessmentCategory;
import com.daacs.model.assessment.ScoringType;
import com.daacs.model.assessment.user.*;
import com.daacs.model.dto.SaveWritingSampleRequest;
import com.daacs.model.dto.UpdateUserAssessmentRequest;
import com.daacs.model.item.ItemGroup;
import com.daacs.model.item.WritingPrompt;
import com.lambdista.util.Try;

import java.time.Instant;
import java.util.List;

/**
 * Created by chostetter on 7/14/16.
 */
public interface UserAssessmentService {
    Try<List<UserAssessmentSummary>> getSummaries(String userId, String assessmentId, Instant takenDate);
    Try<List<UserAssessmentSummary>> getSummariesByGroup(String userId, String groupId, Instant takenDate);
    Try<UserAssessment> createUserAssessment(User user, String assessmentId);
    Try<List<ItemGroup>> getAnswers(String userId, String assessmentId, String domainId, Instant takenDate);
    Try<WritingPrompt> getWritingSample(String userId, String assessmentId, Instant takenDate);
    Try<List<UserAssessmentTakenDate>> getTakenDates(String userId, String groupId);
    Try<WritingPrompt> saveWritingSample(String userId, String assessmentId, SaveWritingSampleRequest saveWritingSampleRequest);
    Try<UserAssessmentSummary> getLatestSummary(String userId, String assessmentId);
    Try<UserAssessmentSummary> getLatestSummaryByGroup(String userId, String groupIds);
    Try<UserAssessment> getUserAssessment(String userId, String userAssessmentId);
    Try<UserAssessment> updateUserAssessment(String userId, List<String> actingUserRoles, UpdateUserAssessmentRequest updateUserAssessmentRequest);
    Try<List<UserAssessment>> gradeUserAssessments(AssessmentCategory[] assessmentCategories, CompletionStatus completionStatus, Instant startDate, Instant endDate, boolean dryRun);
    Try<WritingPrompt> getWritingPrompt(String userId, String assessmentId);
    Try<Void> saveUserAssessment(UserAssessment userAssessment);
    Try<List<UserAssessmentSummary>> getSummaries(List<CompletionStatus> statuses, List<ScoringType> scoringTypes, String userId, int limit, int offset);
    Try<List<UserAssessment>> getUserAssessmentsForManualGrading();
    Try<List<UserAssessmentSummary>> getCompletedUserAssessmentSummaries(Instant startDate, Instant endDate);
    Try<CompletionSummary> getCompletionSummary(String userId);
    Try<List<UserAssessment>> getUserAssessmentsByAssessmentId(String assessmentId);
    Try<Void> bulkUserAssessmentSave(List<UserAssessment> userAssessments);
    Try<UserAssessment> getLatestUserAssessmentIfExists(String userId, String assessmentId);
}
