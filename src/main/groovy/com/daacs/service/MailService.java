package com.daacs.service;

import com.daacs.model.InstructorClass;
import com.daacs.model.User;
import com.lambdista.util.Try;

/**
 * Created by chostetter on 8/3/16.
 */
public interface MailService {
    Try<Void> sendForgotPasswordEmail(String username);
    Try<Void> sendHelpEmail(User user, String assessmentId, String enteredText, String userAgent);
    Try<Void> sendCanvasFailureEmail(User user, Exception exception);
    Try<Void> sendClassInviteEmail(User student, User instructor, InstructorClass instructorClass);
    Try<Void> sendDaacsInviteEmail(String username, String classId, User instructor);
}
