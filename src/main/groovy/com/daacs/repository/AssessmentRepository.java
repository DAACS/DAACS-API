package com.daacs.repository;

import com.daacs.model.assessment.Assessment;
import com.daacs.model.assessment.ScoringType;
import com.lambdista.util.Try;

import java.util.List;
import java.util.Map;

/**
 * Created by chostetter on 7/7/16.
 */
public interface AssessmentRepository {
    Try<Assessment> getAssessment(String id);
    Try<Void> insertAssessment(Assessment assessment);
    Try<List<Assessment>> getAssessments(Boolean enabled, List<String> groupIds);
    Try<Void> saveAssessment(Assessment assessment);
    Try<List<Assessment>> getAssessments(List<ScoringType> scoringTypes, Boolean enabled);
    Try<List<Map>> getAssessmentStats();
}
