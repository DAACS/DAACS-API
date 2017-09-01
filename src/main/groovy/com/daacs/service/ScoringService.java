package com.daacs.service;

import com.daacs.model.assessment.user.CompletionScore;
import com.daacs.model.assessment.user.DomainScore;
import com.daacs.model.assessment.user.UserAssessment;
import com.lambdista.util.Try;

import java.util.List;

/**
 * Created by chostetter on 7/26/16.
 */
public interface ScoringService {
    Try<UserAssessment> autoGradeUserAssessment(UserAssessment userAssessment);
    Try<UserAssessment> manualGradeUserAssessment(String userId, String userAssessmentId, List<DomainScore> domainScores, CompletionScore overallScore);
    boolean canAutoGradeFromUpdate(UserAssessment userAssessment);
}
