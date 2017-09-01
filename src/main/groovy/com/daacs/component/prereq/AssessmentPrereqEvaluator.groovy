package com.daacs.component.prereq

import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.AssessmentSummary
import com.daacs.model.assessment.user.UserAssessment
import com.daacs.model.prereqs.AssessmentPrereq
import com.daacs.model.prereqs.Prerequisite
/**
 * Created by chostetter on 7/14/16.
 */
class AssessmentPrereqEvaluator implements PrereqEvaluator<AssessmentPrereq> {

    List<UserAssessment> latestUserAssessments;

    public AssessmentPrereqEvaluator(List<UserAssessment> latestUserAssessments){
        this.latestUserAssessments = latestUserAssessments;
    }

    public boolean passesPrereq(AssessmentPrereq prerequisite){

        for(UserAssessment userAssessment : latestUserAssessments){
            if(     prerequisite.getAssessmentCategory() == userAssessment.getAssessmentCategory() &&
                    prerequisite.getStatuses().contains(userAssessment.getStatus())){
                return true;
            }
        }

        return false;
    }

    public void evaluatePrereqs(AssessmentSummary assessmentSummary){
        assessmentSummary.setUserPassesPrerequisites(true);
        if(assessmentSummary.getPrerequisites().size() == 0) return;

        for(Prerequisite prerequisite : assessmentSummary.getPrerequisites()){

            if(prerequisite instanceof AssessmentPrereq && !passesPrereq((AssessmentPrereq) prerequisite)){
                assessmentSummary.setUserPassesPrerequisites(false);
                break;
            }

        }
    }

    public List<Prerequisite> getFailedPrereqs(Assessment assessment){

        if(assessment.getPrerequisites().size() == 0) return [];

        List<Prerequisite> failedPrereqs = [];
        for(Prerequisite prerequisite : assessment.getPrerequisites()){

            if(prerequisite instanceof AssessmentPrereq && !passesPrereq((AssessmentPrereq) prerequisite)){
                failedPrereqs.add(prerequisite);
            }

        }

        return failedPrereqs;
    }

}
