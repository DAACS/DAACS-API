package com.daacs.service;

import com.daacs.model.User;
import com.lambdista.util.Try;


/**
 * Created by mgoldman
 */
public interface PendingStudentService {
    Try<Void> inviteStudentToClass(User student);
    Try<Void> inviteStudentToDaacs(String username, String classId, Boolean forceAccept, User instructor);
}
