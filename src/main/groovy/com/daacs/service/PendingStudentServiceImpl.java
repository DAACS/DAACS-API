package com.daacs.service;

import com.daacs.model.*;
import com.daacs.repository.PendingStudentRepository;
import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PendingStudentServiceImpl implements PendingStudentService {
    private static final Logger log = LoggerFactory.getLogger(PendingStudentServiceImpl.class);

    @Autowired
    private PendingStudentRepository pendingStudentRepository;

    @Autowired InstructorClassService instructorClassService;

    @Autowired
    private MailService mailService;

    @Override
    public  Try<Void> inviteStudentToClass(User student) {
        //check if pending student exists
        Try<PendingStudent> maybeStudent = pendingStudentRepository.getPendStudent(student.getUsername());
        if(maybeStudent.isFailure()){
            return new Try.Failure<>(maybeStudent.failed().get());
        }
        PendingStudent pendingStudent = maybeStudent.get();
        if(pendingStudent == null){
            return new Try.Success<Void>(null);
        }

        //send class invite
        Try<Void> maybeInvited = instructorClassService.sendPendingInvite(student, pendingStudent);
        if(maybeInvited.isFailure()){
            return new Try.Failure<>(maybeInvited.failed().get());
        }

        //delete pending student
        return pendingStudentRepository.deletePendStudent(pendingStudent.getId());
    }

    @Override
    public Try<Void> inviteStudentToDaacs(String username, String classId, Boolean forceAccept, User instructor) {
        //check if pending student exists
        Try<PendingStudent> maybeStudent = pendingStudentRepository.getPendStudent(username);
        if(maybeStudent.isFailure()){
            return new Try.Failure<>(maybeStudent.failed().get());
        }
        PendingStudent pendingStudent = maybeStudent.get();

        //create pending student
        if(pendingStudent == null){
            PendingStudent student = new PendingStudent(username, classId, forceAccept);
            Try<Void> maybeCreated = pendingStudentRepository.insertPendStudent(student);
            if(maybeCreated.isFailure()){
                return new Try.Failure<>(maybeCreated.failed().get());
            }

            return mailService.sendDaacsInviteEmail(username, classId, instructor);
        }

        //update pending student
        pendingStudent.addInvite(classId, forceAccept);

        return pendingStudentRepository.updatePendStudent(pendingStudent);
    }
}
