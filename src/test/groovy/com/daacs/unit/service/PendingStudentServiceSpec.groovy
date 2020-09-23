package com.daacs.unit.service

import com.daacs.model.PendingStudent
import com.daacs.model.User
import com.daacs.repository.PendingStudentRepository
import com.daacs.service.*
import com.lambdista.util.Try
import spock.lang.Specification


/**
 * Created by mgoldman
 */
class PendingStudentServiceSpec extends Specification {

    PendingStudentService pendingStudentService
    PendingStudentRepository pendingStudentRepository
    InstructorClassService instructorClassService
    MailService mailService

    User dummyStudent = new User("username", "", "Mr", "Dummy", true, ["ROLE_STUDENT"], "secondaryId", "canvasSisId");
    User dummyInstructor = new User("instructorUsername", "", "Mr", "Instructorator", true, ["ROLE_INSTRUCTOR"], "secondaryId", "");
    PendingStudent dummyPending = new PendingStudent("username", "123", false)

    def setup() {
        dummyStudent.setId(UUID.randomUUID().toString())
        dummyInstructor.setId(UUID.randomUUID().toString())
        pendingStudentRepository = Mock(PendingStudentRepository);
        instructorClassService = Mock(InstructorClassService)
        mailService = Mock(MailService);
        pendingStudentService = new PendingStudentServiceImpl(pendingStudentRepository: pendingStudentRepository, instructorClassService:instructorClassService, mailService: mailService);
    }

    def "inviteStudentToClass: pending student exists, successful"() {
        when:
        Try<Void> maybeResult = pendingStudentService.inviteStudentToClass(dummyStudent)

        then:
        1 * pendingStudentRepository.getPendStudent(_) >> new Try.Success<PendingStudent>(dummyPending)
        1 * instructorClassService.sendPendingInvite(dummyStudent, dummyPending) >> new Try.Success<Void>(null)
        1 * pendingStudentRepository.deletePendStudent(_) >> new Try.Success<Void>(null)

        maybeResult.isSuccess()
    }

    def "inviteStudentToClass: pending student doesn't exists, successful"() {
        when:
        Try<Void> maybeResult = pendingStudentService.inviteStudentToClass(dummyStudent)

        then:
        1 * pendingStudentRepository.getPendStudent(_) >> new Try.Success<PendingStudent>(null)
        0 * instructorClassService.sendPendingInvite(dummyStudent, dummyPending) >> new Try.Success<Void>(null)
        0 * pendingStudentRepository.deletePendStudent(_) >> new Try.Success<Void>(null)

        maybeResult.isSuccess()
    }

    def "inviteStudentToClass: getPendStudent fails"() {
        when:
        Try<Void> maybeResult = pendingStudentService.inviteStudentToClass(dummyStudent)

        then:
        1 * pendingStudentRepository.getPendStudent(_) >> new Try.Failure<PendingStudent>(null)
        0 * instructorClassService.sendPendingInvite(dummyStudent, dummyPending) >> new Try.Success<Void>(null)
        0 * pendingStudentRepository.deletePendStudent(_) >> new Try.Success<Void>(null)

        maybeResult.isFailure()
    }

    def "inviteStudentToClass:  pending student exists, sendPendingInvite fails"() {
        when:
        Try<Void> maybeResult = pendingStudentService.inviteStudentToClass(dummyStudent)

        then:
        1 * pendingStudentRepository.getPendStudent(_) >> new Try.Success<PendingStudent>(dummyPending)
        1 * instructorClassService.sendPendingInvite(dummyStudent, dummyPending) >> new Try.Failure<Void>(null)
        0 * pendingStudentRepository.deletePendStudent(_) >> new Try.Success<Void>(null)

        maybeResult.isFailure()
    }

    def "inviteStudentToClass:  pending student exists, deletePendStudent fails"() {
        when:
        Try<Void> maybeResult = pendingStudentService.inviteStudentToClass(dummyStudent)

        then:
        1 * pendingStudentRepository.getPendStudent(_) >> new Try.Success<PendingStudent>(dummyPending)
        1 * instructorClassService.sendPendingInvite(dummyStudent, dummyPending) >> new Try.Success<Void>(null)
        1 * pendingStudentRepository.deletePendStudent(_) >> new Try.Failure<Void>(null)

        maybeResult.isFailure()
    }

    def "inviteStudentToDaacs: successful"() {
        when:
        Try<Void> maybeResult = pendingStudentService.inviteStudentToDaacs("username", "123", false, dummyInstructor)

        then:
        1 * pendingStudentRepository.getPendStudent(_)  >> new Try.Success<PendingStudent>(null)
        1 * pendingStudentRepository.insertPendStudent(_) >> new Try.Success<Void>(null)
        1 * mailService.sendDaacsInviteEmail("username", "123", dummyInstructor) >> new Try.Success<Void>(null)

        maybeResult.isSuccess()
    }

    def "inviteStudentToDaacs: pending student already exists is updated, successful"() {
        when:
        Try<Void> maybeResult = pendingStudentService.inviteStudentToDaacs("username", "123", false, dummyInstructor)

        then:
        1 * pendingStudentRepository.getPendStudent(_)  >> new Try.Success<PendingStudent>(dummyPending)
        0 * pendingStudentRepository.insertPendStudent(_) >> new Try.Success<Void>(null)
        0 * mailService.sendDaacsInviteEmail("username", "123", dummyInstructor) >> new Try.Success<Void>(null)
        1 * pendingStudentRepository.updatePendStudent(_) >> new Try.Success<Void>(null)

        maybeResult.isSuccess()
    }

    def "inviteStudentToDaacs: getPendStudent fails"() {
        when:
        Try<Void> maybeResult = pendingStudentService.inviteStudentToDaacs("username", "123", false, dummyInstructor)

        then:
        1 * pendingStudentRepository.getPendStudent(_)  >> new Try.Failure<PendingStudent>(null)
        0 * pendingStudentRepository.insertPendStudent(_) >> new Try.Success<Void>(null)
        0 * mailService.sendDaacsInviteEmail("username", "123", dummyInstructor) >> new Try.Success<Void>(null)
        0 * pendingStudentRepository.updatePendStudent(_) >> new Try.Success<Void>(null)

        maybeResult.isFailure()
    }

    def "inviteStudentToDaacs: pending student already exists updatePendStudent fails"() {
        when:
        Try<Void> maybeResult = pendingStudentService.inviteStudentToDaacs("username", "123", false, dummyInstructor)

        then:
        1 * pendingStudentRepository.getPendStudent(_)  >> new Try.Success<PendingStudent>(dummyPending)
        0 * pendingStudentRepository.insertPendStudent(_) >> new Try.Success<Void>(null)
        0 * mailService.sendDaacsInviteEmail("username", "123", dummyInstructor) >> new Try.Success<Void>(null)
        1 * pendingStudentRepository.updatePendStudent(_) >> new Try.Failure<Void>(null)

        maybeResult.isFailure()
    }

    def "inviteStudentToDaacs: insertPendStudent fails"() {
        when:
        Try<Void> maybeResult = pendingStudentService.inviteStudentToDaacs("username", "123", false, dummyInstructor)

        then:
        1 * pendingStudentRepository.getPendStudent(_)  >> new Try.Success<PendingStudent>(null)
        1 * pendingStudentRepository.insertPendStudent(_) >> new Try.Failure<Void>(null)
        0 * mailService.sendDaacsInviteEmail("username", "123", dummyInstructor) >> new Try.Success<Void>(null)

        maybeResult.isFailure()
    }

    def "inviteStudentToDaacs: sendDaacsInviteEmail fails"() {
        when:
        Try<Void> maybeResult = pendingStudentService.inviteStudentToDaacs("username", "123", false, dummyInstructor)

        then:
        1 * pendingStudentRepository.getPendStudent(_)  >> new Try.Success<PendingStudent>(null)
        1 * pendingStudentRepository.insertPendStudent(_) >> new Try.Success<Void>(null)
        1 * mailService.sendDaacsInviteEmail("username", "123", dummyInstructor) >> new Try.Failure<Void>(null)

        maybeResult.isFailure()
    }

}
