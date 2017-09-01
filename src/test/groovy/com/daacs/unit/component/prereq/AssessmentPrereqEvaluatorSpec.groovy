package com.daacs.component.prereq

import com.daacs.model.assessment.*
import com.daacs.model.assessment.user.CATUserAssessment
import com.daacs.model.assessment.user.CompletionStatus
import com.daacs.model.assessment.user.UserAssessment
import com.daacs.model.assessment.user.UserAssessmentSummary
import com.daacs.model.prereqs.AssessmentPrereq
import com.daacs.model.prereqs.Prerequisite
import spock.lang.Specification

/**
 * Created by chostetter on 6/22/16.
 */
class AssessmentPrereqEvaluatorSpec extends Specification {

    AssessmentPrereqEvaluator assessmentPrereqEvaluator

    List<UserAssessment> userAssessments

    def setup(){
        userAssessments = [
                new CATUserAssessment(
                        assessmentCategory: AssessmentCategory.MATHEMATICS,
                        status: CompletionStatus.GRADED
                )
        ]

        assessmentPrereqEvaluator = new AssessmentPrereqEvaluator(userAssessments)
    }

    def "passesPrereq evaluates prereq successfully"() {
        setup:
        assessmentPrereqEvaluator = new AssessmentPrereqEvaluator(userAssessments)
        Prerequisite alreadyTookMath = new AssessmentPrereq(
                assessmentCategory: AssessmentCategory.MATHEMATICS,
                statuses: [CompletionStatus.GRADED])

        when:
        boolean passes = assessmentPrereqEvaluator.passesPrereq(alreadyTookMath)

        then:
        passes
    }

    def "passesPrereq evaluates prereq successfully with multiple statuses" () {
        setup:
        assessmentPrereqEvaluator = new AssessmentPrereqEvaluator(userAssessments)
        Prerequisite alreadyTookMath = new AssessmentPrereq(
                assessmentCategory: AssessmentCategory.MATHEMATICS,
                statuses: [CompletionStatus.GRADED, CompletionStatus.COMPLETED])

        when:
        boolean passes = assessmentPrereqEvaluator.passesPrereq(alreadyTookMath)

        then:
        passes
    }

    def "passesPrereq evaluates prereq successfully, but does not pass" () {
        setup:
        userAssessments.get(0).status = CompletionStatus.IN_PROGRESS
        assessmentPrereqEvaluator = new AssessmentPrereqEvaluator(userAssessments)

        Prerequisite alreadyTookMath = new AssessmentPrereq(assessmentCategory: AssessmentCategory.MATHEMATICS, statuses: [CompletionStatus.GRADED, CompletionStatus.COMPLETED])

        when:
        boolean passes = assessmentPrereqEvaluator.passesPrereq(alreadyTookMath)

        then:
        !passes
    }

    def "passesPrereq evaluates prereq successfully, but does not pass #2" () {
        setup:
        assessmentPrereqEvaluator = new AssessmentPrereqEvaluator(userAssessments)
        Prerequisite alreadyTookMath = new AssessmentPrereq(assessmentCategory: AssessmentCategory.WRITING, statuses: [CompletionStatus.GRADED, CompletionStatus.COMPLETED])

        when:
        boolean passes = assessmentPrereqEvaluator.passesPrereq(alreadyTookMath)

        then:
        !passes
    }

    def "evaluatePrereqs: no prereqs"(){
        AssessmentSummary assessmentSummary = new AssessmentSummary(
                assessmentCategory: AssessmentCategory.MATHEMATICS,
                userAssessmentSummary: new UserAssessmentSummary(status: CompletionStatus.GRADED),
                prerequisites: []
        )

        when:
        assessmentPrereqEvaluator.evaluatePrereqs(assessmentSummary)

        then:
        assessmentSummary.userPassesPrerequisites
    }

    def "evaluatePrereqs: already took math"(){
        AssessmentSummary assessmentSummary = new AssessmentSummary(
                assessmentCategory: AssessmentCategory.WRITING,
                prerequisites: [
                        new AssessmentPrereq(assessmentCategory: AssessmentCategory.MATHEMATICS, statuses: [CompletionStatus.GRADED, CompletionStatus.COMPLETED])
                ]
        )

        when:
        assessmentPrereqEvaluator.evaluatePrereqs(assessmentSummary)

        then:
        assessmentSummary.userPassesPrerequisites
    }

    def "evaluatePrereqs: already took math and reading"(){
        AssessmentSummary assessmentSummary = new AssessmentSummary(
                assessmentCategory: AssessmentCategory.WRITING,
                prerequisites: [
                        new AssessmentPrereq(assessmentCategory: AssessmentCategory.MATHEMATICS, statuses: [CompletionStatus.GRADED, CompletionStatus.COMPLETED]),
                        new AssessmentPrereq(assessmentCategory: AssessmentCategory.READING, statuses: [CompletionStatus.GRADED, CompletionStatus.COMPLETED])
                ]
        )

        when:
        assessmentPrereqEvaluator.evaluatePrereqs(assessmentSummary)

        then:
        !assessmentSummary.userPassesPrerequisites
    }

    def "getFailedPrereqs: returns empty when assessment's prereqs are empty"(){
        when:
        List<Prerequisite> failedPrereqs = assessmentPrereqEvaluator.getFailedPrereqs(new CATAssessment(prerequisites: []))

        then:
        failedPrereqs.size() == 0
    }

    def "getFailedPrereqs: returns failed prereq"(){
        setup:
        Assessment assessment = new MultipleChoiceAssessment(
                prerequisites: [
                        new AssessmentPrereq(assessmentCategory: AssessmentCategory.READING, statuses: [CompletionStatus.GRADED, CompletionStatus.COMPLETED])
                ]
        )

        when:
        List<Prerequisite> failedPrereqs = assessmentPrereqEvaluator.getFailedPrereqs(assessment)

        then:
        failedPrereqs.size() == 1
        failedPrereqs.get(0) == assessment.getPrerequisites().get(0)
    }
}
