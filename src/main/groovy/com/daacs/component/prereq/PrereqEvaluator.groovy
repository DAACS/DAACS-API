package com.daacs.component.prereq

import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.AssessmentSummary
import com.daacs.model.prereqs.Prerequisite

/**
 * Created by chostetter on 7/14/16.
 */
public interface PrereqEvaluator<T extends Prerequisite> {
    boolean passesPrereq(T prerequisite);
    void evaluatePrereqs(AssessmentSummary assessmentSummary);
    List<Prerequisite> getFailedPrereqs(Assessment assessment);
}
