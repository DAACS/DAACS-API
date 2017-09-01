package com.daacs.service;

import com.daacs.model.assessment.user.UserAssessment;
import com.lambdista.util.Try;

/**
 * Created by chostetter on 8/11/16.
 */
public interface MessageService {
    Try<Void> queueUserAssessmentForGrading(UserAssessment userAssessment);
    Try<Void> queueCanvasSubmissionUpdate(String userId);
    Try<Void> queueCanvasSubmissionUpdateForAllStudents(Boolean resetCompletionFlags);
}
