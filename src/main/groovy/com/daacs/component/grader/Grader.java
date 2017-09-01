package com.daacs.component.grader;

import com.daacs.model.assessment.Assessment;
import com.daacs.model.assessment.user.CompletionScore;
import com.daacs.model.assessment.user.DomainScore;
import com.daacs.model.assessment.user.UserAssessment;
import com.lambdista.util.Try;

import java.util.List;

/**
 * Created by chostetter on 7/27/16.
 */
public interface Grader<U extends UserAssessment, T extends Assessment> {
    Try<U> grade();
    Try<CompletionScore> getAverageCompletionScore(List<DomainScore> domainScores);
}
