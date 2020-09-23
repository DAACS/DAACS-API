package com.daacs.model

import org.apache.commons.lang3.StringUtils
import org.springframework.data.annotation.Id

import javax.validation.constraints.NotNull

/**
 * Created by mgoldman on 6/8/20.
 */
class InstructorClass {
    @Id
    String id = UUID.randomUUID().toString();
    @NotNull
    String name
    @NotNull
    String instructorId

    List<String> assessmentIds = new ArrayList<>();

    Boolean canEditAssessments = true

    List<StudentClassInvite> studentInvites = new ArrayList<>();

    InstructorClass() {
    }

    InstructorClass(String name, String instructorId) throws MissingFieldException {
        if(StringUtils.isBlank(name) || StringUtils.isBlank(instructorId) ){
            throw new MissingFieldException("class name must not be blank", "name", InstructorClass)
        }
        if(StringUtils.isBlank(instructorId) ){
            throw new MissingFieldException("instructorId can not be blank", "name", InstructorClass)
        }

        this.name = name
        this.instructorId = instructorId
    }

    void addAssessmentId(String assessmentId) {
         if (!this.assessmentIds.contains(assessmentId)){
            this.assessmentIds.add(assessmentId)
        }
    }

    void addStudentInvite(StudentClassInvite studentClassInvite) {
        if(studentInvites == null){
            this.studentInvites = new ArrayList<>();
            this.studentInvites.add(studentClassInvite)
        }
        else{
            for(StudentClassInvite currentClassInvite: this.studentInvites){
                if(currentClassInvite.getStudentId().equals(studentClassInvite.getStudentId())){
                    currentClassInvite.setInviteStatusAccepted(studentClassInvite.getInviteStatusAccepted())
                    return
                }
            }
            this.studentInvites.add(studentClassInvite)
        }
    }
}
