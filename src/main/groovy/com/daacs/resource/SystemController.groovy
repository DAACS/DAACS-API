package com.daacs.resource

import com.daacs.framework.exception.BadInputException
import com.daacs.framework.exception.NotEnabledException
import com.daacs.framework.exception.NotFoundException
import com.daacs.framework.exception.RepoNotFoundException
import com.daacs.model.ErrorResponse
import com.daacs.model.User
import com.daacs.model.assessment.AssessmentCategory
import com.daacs.model.assessment.user.CompletionStatus
import com.daacs.model.assessment.user.UserAssessmentSummary
import com.daacs.model.assessment.user.UserAssessment
import com.daacs.model.assessment.user.CompletionSummary
import com.daacs.service.CanvasService
import com.daacs.service.MessageService
import com.daacs.service.UserAssessmentService
import com.daacs.service.UserService
import com.lambdista.util.Try
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import java.time.Instant
/**
 * Created by chostetter on 7/5/16.
 */
@RestController
@RequestMapping(value = "/system", produces = "application/json")
public class SystemController extends AuthenticatedController{

    @Autowired
    private UserAssessmentService userAssessmentService

    @Autowired
    private UserService userService

    @Autowired
    private MessageService messageService

    @Autowired
    private CanvasService canvasService

    @ApiOperation(
            value = "list of completed user assessment summaries",
            response = UserAssessmentSummary,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/completed-user-assessments", method = RequestMethod.GET, params = ["startDate", "endDate"], produces = "application/json")
    public List<UserAssessmentSummary> getCompletedUserAssessments(
            @RequestParam(value = "startDate") String startDateString,
            @RequestParam(value = "endDate") String endDateString){

        checkPermissions([ROLE_SYSTEM]);

        Instant startDate = Instant.parse(startDateString);
        Instant endDate = Instant.parse(endDateString);

        Try<List<UserAssessmentSummary>> maybeUserAssessmentSummaries = userAssessmentService.getCompletedUserAssessmentSummaries(startDate, endDate);
        if(maybeUserAssessmentSummaries.isFailure()){
            throw maybeUserAssessmentSummaries.failed().get()
        }

        return maybeUserAssessmentSummaries.get();
    }

    @ApiOperation(
            value = "check if a user has completed all assessment types",
            response = CompletionSummary
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/completion-summary", method = RequestMethod.GET, produces = "application/json")
    public CompletionSummary getUserCompletionSummary(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "secondaryId", required = false) String secondaryId) {

        checkPermissions([ROLE_SYSTEM]);
        if((userId == null && username == null && secondaryId == null) || (userId != null && username != null && secondaryId != null)){
            throw new BadInputException("parameter", "must specify one (and only one) of the following parameters: userId, username, secondaryId")
        }

        User user;
        if(username != null){
            user = userService.getUserByUsername(username).get();
        }
        else if(secondaryId != null){
            user = userService.getUserBySecondaryId(secondaryId).get();
        }
        else{
            user = userService.getUser(userId).get();
        }

        if(user == null) {
            RepoNotFoundException ex = new RepoNotFoundException("User");
            ex.setLog(false);
            throw ex;
        }

        return userAssessmentService.getCompletionSummary(user.getId()).checkedGet();
    }

    @ApiOperation(
            value = "Queue a CanvasSubmissionMessage for every student"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/queue-canvas-submission-updates", method = RequestMethod.PUT, produces = "application/json")
    public void queueCanvasSubmissionUpdates(
            @RequestParam(value = "resetCompletionFlags", required = false) Boolean resetCompletionFlags){
        checkPermissions([ROLE_SYSTEM]);

        if(!canvasService.isEnabled()){
            throw new NotEnabledException("canvas", "Canvas is not enabled");
        }

        if(resetCompletionFlags == null){
            resetCompletionFlags = false;
        }

        messageService.queueCanvasSubmissionUpdateForAllStudents(resetCompletionFlags).checkedGet();
    }

    @ApiOperation(
            value = "Calculate grades for an assessment category between a start and end date"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/calculate-grades", method = RequestMethod.GET, produces = "application/json")
    public void recalculateGrades(
            @RequestParam(value = "assessmentCategories[]", required = false) AssessmentCategory[] assessmentCategories,
            @RequestParam(value = "completionStatus", required = false, defaultValue="GRADED") CompletionStatus completionStatus,
            @RequestParam(value = "startDate", required = true) String startDateString,
            @RequestParam(value = "endDate", required = true) String endDateString,
            @RequestParam(value = "dryRun", required = false, defaultValue = "true") Boolean dryRun){

        checkPermissions([ROLE_SYSTEM]);

        Instant startDate = Instant.parse(startDateString);
        Instant endDate = Instant.parse(endDateString);

        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentService.gradeUserAssessments(assessmentCategories, completionStatus, startDate, endDate, dryRun);
        if(maybeUserAssessments.isFailure()){
            throw maybeUserAssessments.failed().get()
        }
    }
}
