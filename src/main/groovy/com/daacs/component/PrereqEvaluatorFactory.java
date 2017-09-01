package com.daacs.component;

import com.daacs.component.prereq.AssessmentPrereqEvaluator;
import com.daacs.model.assessment.user.UserAssessment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by chostetter on 7/14/16.
 */
@Component
public class PrereqEvaluatorFactory {

    public AssessmentPrereqEvaluator getAssessmentPrereqEvaluator(List<UserAssessment> userAssessments){
        return new AssessmentPrereqEvaluator(userAssessments);
    }

}
