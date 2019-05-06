package com.daacs.unit.component

import com.daacs.component.PrereqEvaluatorFactory
import com.daacs.component.prereq.AssessmentPrereqEvaluator
import com.daacs.component.prereq.PrereqEvaluator
import spock.lang.Specification
/**
 * Created by chostetter on 6/22/16.
 */
class PrereqEvaluatorFactorySpec extends Specification {

    PrereqEvaluatorFactory prereqEvaluatorFactory

    def setup() {
        prereqEvaluatorFactory = new PrereqEvaluatorFactory()
    }

    def "getAssessmentPrereqEvaluator returns AssessmentPrereqEvaluator"() {
        when:
        PrereqEvaluator prereqEvaluator = prereqEvaluatorFactory.getAssessmentPrereqEvaluator([])

        then:
        prereqEvaluator instanceof AssessmentPrereqEvaluator
    }
}
