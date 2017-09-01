package com.daacs.service;

import com.daacs.model.User;
import com.daacs.model.assessment.user.UserAssessment;
import com.daacs.model.queue.CanvasSubmissionMessage;
import com.daacs.model.queue.GradingMessage;
import com.daacs.repository.MessageRepository;
import com.lambdista.util.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Created by chostetter on 8/11/16.
 */
@Service
public class MessageServiceImpl implements MessageService {
    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserService userService;

    @Override
    public Try<Void> queueUserAssessmentForGrading(UserAssessment userAssessment) {
        GradingMessage message = new GradingMessage();
        message.setUserAssessmentId(userAssessment.getId());
        message.setUserId(userAssessment.getUserId());

        return messageRepository.insertMessage(message);
    }

    @Override
    public Try<Void> queueCanvasSubmissionUpdate(String userId) {
        CanvasSubmissionMessage message = new CanvasSubmissionMessage();
        message.setUserId(userId);

        return messageRepository.insertMessage(message);
    }

    @Override
    public Try<Void> queueCanvasSubmissionUpdateForAllStudents(Boolean resetCompletionFlags) {
        Try<List<User>> maybeUsers = userService.getUsers(Arrays.asList("ROLE_STUDENT"));
        if(maybeUsers.isFailure()){
            return new Try.Failure<>(maybeUsers.failed().get());
        }

        for(User user : maybeUsers.get()){
            if(resetCompletionFlags){
                user.setReportedCompletionToCanvas(false);
                Try<User> maybeSavedUser = userService.saveUser(user);
                if(maybeSavedUser.isFailure()){
                    return new Try.Failure<>(maybeSavedUser.failed().get());
                }
            }

            CanvasSubmissionMessage message = new CanvasSubmissionMessage();
            message.setUserId(user.getId());

            Try<Void> maybeInserted = messageRepository.insertMessage(message);
            if(maybeInserted.isFailure()){
                return new Try.Failure<>(maybeInserted.failed().get());
            }
        }

        return new Try.Success<>(null);
    }
}
