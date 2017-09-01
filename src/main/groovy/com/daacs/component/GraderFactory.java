package com.daacs.component;

import com.daacs.component.grader.Grader;
import com.daacs.component.grader.ItemGroupGrader;
import com.daacs.component.grader.WritingPromptGrader;
import com.daacs.model.assessment.*;
import com.daacs.model.assessment.user.CATUserAssessment;
import com.daacs.model.assessment.user.MultipleChoiceUserAssessment;
import com.daacs.model.assessment.user.WritingPromptUserAssessment;
import com.daacs.service.LightSideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Created by chostetter on 7/14/16.
 */

@Configuration
@Component
public class GraderFactory {

    @Autowired
    private LightSideService lightSideService;

    public Grader getGrader(MultipleChoiceUserAssessment userAssessment, MultipleChoiceAssessment assessment){
        return new ItemGroupGrader<>(userAssessment, assessment);
    }

    public Grader getGrader(CATUserAssessment userAssessment, CATAssessment assessment){
        return new ItemGroupGrader<>(userAssessment, assessment);
    }

    public Grader getGrader(WritingPromptUserAssessment userAssessment, WritingAssessment assessment){
        return new WritingPromptGrader(userAssessment, assessment, lightSideService);
    }

}
