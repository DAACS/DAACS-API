package com.daacs.model.assessment.user

import com.daacs.model.item.WritingPrompt
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
/**
 * Created by chostetter on 7/5/16.
 */

@JsonIgnoreProperties(["metaClass"])
public class WritingPromptUserAssessment extends UserAssessment{

    WritingPrompt writingPrompt;

}
