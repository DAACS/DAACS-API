package com.daacs.component.queuing.handlers;

import com.daacs.component.queuing.Retry;
import com.daacs.model.assessment.ScoringType;
import com.daacs.model.assessment.user.CompletionStatus;
import com.daacs.model.assessment.user.UserAssessment;
import com.daacs.model.queue.GradingMessage;
import com.daacs.model.queue.QueueMessage;
import com.daacs.service.ScoringService;
import com.daacs.service.UserAssessmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chostetter on 8/11/16.
 */

@Component
public class GradingMessageHandler implements  MessageHandler<GradingMessage> {
    private static final Logger log = LoggerFactory.getLogger(GradingMessageHandler.class);

    @Autowired
    private ScoringService scoringService;

    @Autowired
    private UserAssessmentService userAssessmentService;

    @Autowired
    private ObjectMapper objectMapper;

    private Retry retry = new Retry(5, 5);
    private List<CompletionStatus> processableStatuses = new ArrayList<CompletionStatus>(){{
        add(CompletionStatus.COMPLETED);
        add(CompletionStatus.GRADED);
    }};

    @SuppressWarnings("unchecked")
    @Override
    public Try<Void> handleMessage(GradingMessage gradingMessage) throws Exception {
        log.info("Processing GradingMessage: {}", objectMapper.writeValueAsString(gradingMessage));


        Try<UserAssessment> maybeUserAssessment = userAssessmentService.getUserAssessment(gradingMessage.getUserId(), gradingMessage.getUserAssessmentId());
        if(maybeUserAssessment.isFailure()){
            return new Try.Failure<>(maybeUserAssessment.failed().get());
        }

        UserAssessment userAssessment = maybeUserAssessment.get();

        if(!processableStatuses.contains(userAssessment.getStatus())){
            log.info("UserAssessment {} with status {} is not a valid status (COMPLETED/GRADED), skipping", userAssessment.getId(), userAssessment.getStatus());
            return new Try.Success<>(null);
        }

        if (userAssessment.getScoringType() == ScoringType.MANUAL) {
            log.info("UserAssessment {} with status {} requires Manual grading, skipping", userAssessment.getId(), userAssessment.getStatus());
            return new Try.Success<>(null);
        }

        UserAssessment updatedUserAssessment;
        try{
            updatedUserAssessment = retry.execute(() -> scoringService.autoGradeUserAssessment(userAssessment));
        }
        catch(Exception e){
            updatedUserAssessment = userAssessment;
            updatedUserAssessment.setStatus(CompletionStatus.GRADING_FAILURE);
            updatedUserAssessment.setGradingError(e.getMessage());

            log.error(MessageFormat.format("UserAssessment {0} failed grading: {1}", updatedUserAssessment.getId(), e.getMessage()), e);
        }

        Try<Void> maybeResults = userAssessmentService.saveUserAssessment(updatedUserAssessment);
        if(maybeResults.isFailure()){
            return new Try.Failure<>(maybeResults.failed().get());
        }

        return new Try.Success<>(null);
    }

    @Override
    public boolean canHandle(QueueMessage queueMessage) {
        return queueMessage instanceof GradingMessage;
    }
}
