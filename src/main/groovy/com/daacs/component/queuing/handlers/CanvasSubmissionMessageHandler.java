package com.daacs.component.queuing.handlers;

import com.daacs.component.queuing.Retry;
import com.daacs.framework.exception.InvalidObjectException;
import com.daacs.model.User;
import com.daacs.model.assessment.user.CompletionSummary;
import com.daacs.model.queue.CanvasSubmissionMessage;
import com.daacs.model.queue.QueueMessage;
import com.daacs.service.CanvasService;
import com.daacs.service.MailService;
import com.daacs.service.UserAssessmentService;
import com.daacs.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

/**
 * Created by chostetter on 4/6/17.
 */

@Component
public class CanvasSubmissionMessageHandler implements MessageHandler<CanvasSubmissionMessage> {
    private static final Logger log = LoggerFactory.getLogger(CanvasSubmissionMessageHandler.class);

    @Autowired
    private CanvasService canvasService;

    @Autowired
    private UserAssessmentService userAssessmentService;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MailService mailService;

    private Retry retry = new Retry(5, 5);

    @SuppressWarnings("unchecked")
    @Override
    public Try<Void> handleMessage(CanvasSubmissionMessage canvasSubmissionMessage) throws Exception {
        log.info("Processing CanvasSubmissionMessage: {}", objectMapper.writeValueAsString(canvasSubmissionMessage));
        if(!canvasService.isEnabled()){
            log.info("Canvas not enabled, skipping");
            return new Try.Success<>(null);
        }

        String userId = canvasSubmissionMessage.getUserId();
        Try<User> maybeUser = userService.getUser(userId);
        if(maybeUser.isFailure()){
            return new Try.Failure<>(maybeUser.failed().get());
        }

        User user = maybeUser.get();
        if(user.getReportedCompletionToCanvas()){
            log.info("Completion has already been reported to Canvas for user {}, skipping", userId);
            return new Try.Success<>(null);
        }

        Try<CompletionSummary> maybeCompletionSummary = userAssessmentService.getCompletionSummary(userId);
        if(maybeCompletionSummary.isFailure()){
            return new Try.Failure<>(maybeCompletionSummary.failed().get());
        }

        if(!maybeCompletionSummary.get().getHasCompletedAllCategories()){
            log.info("User {} has not completed all assessment categories, skipping", userId);
            return new Try.Success<>(null);
        }

        if(user.getCanvasSisId() == null){
            log.error("User {} has completed all assessment categories but does not have an SIS ID for Canvas, skipping", userId);
            return new Try.Failure<>(new InvalidObjectException("User", "User w/id " + user.getId() + " does not have an SIS ID for Canvas"));
        }

        try {
            String canvasResponse = retry.execute(() -> canvasService.markAssignmentCompleted(user.getCanvasSisId()));
            log.info("Successfully notified Canvas of DAACS completion for user {} w/response: {}", userId, canvasResponse);

            user.setReportedCompletionToCanvas(true);
            Try<User> maybeSavedUser = userService.saveUser(user);
            if(maybeSavedUser.isFailure()){
                return new Try.Failure<>(maybeSavedUser.failed().get());
            }
        }
        catch(Exception ex){
            log.error(MessageFormat.format("Failed communicating with Canvas: {0}", ex.getMessage()), ex);

            log.info("Sending email for Canvas failure");
            Try<Void> maybeSentEmail = mailService.sendCanvasFailureEmail(user, ex);
            if(maybeSentEmail.isFailure()){
                return new Try.Failure<>(maybeSentEmail.failed().get());
            }
        }

        return new Try.Success<>(null);
    }

    @Override
    public boolean canHandle(QueueMessage queueMessage) {
        return queueMessage instanceof CanvasSubmissionMessage;
    }
}
