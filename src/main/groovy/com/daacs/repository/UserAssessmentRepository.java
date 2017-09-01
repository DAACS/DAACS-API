package com.daacs.repository;

import com.daacs.model.assessment.AssessmentCategory;
import com.daacs.model.assessment.ScoringType;
import com.daacs.model.assessment.user.CompletionStatus;
import com.daacs.model.assessment.user.UserAssessment;
import com.lambdista.util.Try;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Created by chostetter on 7/7/16.
 */
public interface UserAssessmentRepository {
    Try<UserAssessment> getUserAssessment(String userId, String assessmentId, Instant takenDate);
    Try<UserAssessment> getLatestUserAssessment(String userId, String assessmentId);
    Try<UserAssessment> getLatestUserAssessment(String userId, AssessmentCategory assessmentCategory);
    Try<List<UserAssessment>> getLatestUserAssessments(String userId, List<String> assessmentIds);
    Try<List<UserAssessment>> getUserAssessments(String userId, String assessmentId, Instant takenDate);
    Try<List<UserAssessment>> getUserAssessments(String userId, String assessmentId);
    Try<List<UserAssessment>> getUserAssessments(String userId, AssessmentCategory assessmentCategory, Instant takenDate);
    Try<List<UserAssessment>> getUserAssessments(String userId);
    Try<Map<AssessmentCategory, List<UserAssessment>>> getUserAssessmentsByCategory(String userId);
    Try<Void> saveUserAssessment(UserAssessment userAssessment);
    Try<Void> insertUserAssessment(UserAssessment userAssessment);
    Try<UserAssessment> getUserAssessmentById(String userId, String userAssessmentId);
    Try<UserAssessment> getLatestUserAssessment(String userId);
    Try<List<UserAssessment>> getUserAssessments(List<CompletionStatus> statuses, List<ScoringType> scoringTypes, String userId, Integer limit, Integer offset);
    Try<List<UserAssessment>> getUserAssessments(List<CompletionStatus> statuses, List<String> assessmentIds);
    Try<List<UserAssessment>> getCompletedUserAssessments(Instant startDate, Instant endDate);
    public Try<List<UserAssessment>> getUserAssessments(AssessmentCategory[] assessmentCategory, CompletionStatus completionStatus, Instant startDate, Instant endDate);
}
