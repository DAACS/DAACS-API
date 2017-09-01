package com.daacs.unit.component

import com.daacs.component.GraderFactory
import com.daacs.component.grader.Grader
import com.daacs.component.grader.ItemGroupGrader
import com.daacs.component.grader.WritingPromptGrader
import com.daacs.model.assessment.CATAssessment
import com.daacs.model.assessment.MultipleChoiceAssessment
import com.daacs.model.assessment.WritingAssessment
import com.daacs.model.assessment.user.CATUserAssessment
import com.daacs.model.assessment.user.MultipleChoiceUserAssessment
import com.daacs.model.assessment.user.WritingPromptUserAssessment
import spock.lang.Specification
/**
 * Created by chostetter on 6/22/16.
 */
class GraderFactorySpec extends Specification {

    GraderFactory graderFactory

    def setup() {
        graderFactory = new GraderFactory()
    }

    def "getGrader returns ItemGroupGrader (MultipleChoice)"() {
        when:
        Grader grader = graderFactory.getGrader(new MultipleChoiceUserAssessment(), new MultipleChoiceAssessment())

        then:
        grader instanceof ItemGroupGrader
    }

    def "getGrader returns ItemGroupGrader (CAT)"() {
        when:
        Grader grader = graderFactory.getGrader(new CATUserAssessment(), new CATAssessment())

        then:
        grader instanceof ItemGroupGrader
    }

    def "getGrader returns WritingPromptGrader (WritingPrompt)"() {
        when:
        Grader grader = graderFactory.getGrader(new WritingPromptUserAssessment(), new WritingAssessment())

        then:
        grader instanceof WritingPromptGrader
    }
}
