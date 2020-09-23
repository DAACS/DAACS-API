package com.daacs.model


class StudentClassInvite {
    StudentClassInvite() {
    }

    StudentClassInvite(String studentId) {
        this.studentId = studentId
    }

    String studentId
    Boolean inviteStatusAccepted = false

}
