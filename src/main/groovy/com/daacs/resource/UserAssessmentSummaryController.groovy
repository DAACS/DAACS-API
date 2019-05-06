package com.daacs.resource

import com.daacs.framework.exception.NotFoundException
import com.daacs.framework.exception.RepoNotFoundException
import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.AssessmentCategory
import com.daacs.model.assessment.ScoringType
import com.daacs.model.assessment.user.CompletionStatus
import com.daacs.model.assessment.user.UserAssessmentSummary
import com.daacs.model.dto.ScoringClass
import com.daacs.service.UserAssessmentService
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
 * Created by chostetter on 7/12/16.
 */
@RestController
@RequestMapping(value = "/user-assessment-summaries", produces = "application/json")
class UserAssessmentSummaryController extends AuthenticatedController {

    @Autowired
    UserAssessmentService userAssessmentService;

    @ApiOperation(
            value = "list user assessment summaries",
            response = UserAssessmentSummary,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.GET, params = ["assessmentId"], produces = "application/json")
    public List<UserAssessmentSummary> getSummaries(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "assessmentId") String assessmentId,
            @RequestParam(value = "takenDate", required = false) String takenDate){

        Instant takenDateInstant = null;
        if(takenDate != null){
            takenDateInstant = Instant.parse(takenDate)
        }

        return userAssessmentService.getSummaries(determineUserId(userId), assessmentId, takenDateInstant).checkedGet();
    }

    @ApiOperation(
            value = "list user assessment summaries for a category",
            response = UserAssessmentSummary,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.GET, params = ["assessmentCategoryGroupId"], produces = "application/json")
    public List<UserAssessmentSummary> getSummariesByCategory(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "assessmentCategoryGroupId") String groupId,
            @RequestParam(value = "takenDate", required = true) String takenDate){

        Instant takenDateInstant = null;
        if(takenDate != null){
            takenDateInstant = Instant.parse(takenDate)
        }

        return userAssessmentService.getSummariesByGroup(determineUserId(userId), groupId, takenDateInstant).checkedGet();
    }

    @ApiOperation(
            value = "list user assessment summaries by status",
            response = UserAssessmentSummary,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.GET, params = ["status"], produces = "application/json")
    public List<UserAssessmentSummary> getSummaries(
            @RequestParam(value = "status") List<CompletionStatus> statuses,
            @RequestParam(value = "scoring", required = false) ScoringClass scoringClass,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset){

        checkPermissions([ROLE_ADMIN]);

        if(limit == null){
            limit = 10;
        }

        if(limit > 100){
            limit = 100;
        }

        if(offset == null){
            offset = 0;
        }

        List<ScoringType> scoringTypes = (scoringClass != null? scoringClass.getScoringTypes() : null);

        Try<List<UserAssessmentSummary>> maybeUserAssessmentSummaries = userAssessmentService.getSummaries(statuses, scoringTypes, userId, limit, offset);
        if(maybeUserAssessmentSummaries.isFailure()){
            throw maybeUserAssessmentSummaries.failed().get();
        }

        return maybeUserAssessmentSummaries.get();
    }

    @ApiOperation(
            value = "get latest user assessment summary",
            response = UserAssessmentSummary,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.GET, params = ["assessmentId", "limit=1"], produces = "application/json")
    public List<UserAssessmentSummary> getLatestSummary(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "assessmentId") String assessmentId){

        UserAssessmentSummary userAssessmentSummary = userAssessmentService.getLatestSummary(determineUserId(userId), assessmentId).checkedGet();

        return userAssessmentSummary != null? [userAssessmentSummary] : [];
    }

    @ApiOperation(
            value = "get latest user assessment summary",
            response = UserAssessmentSummary,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.GET, params = ["assessmentCategoryGroupId", "limit=1"], produces = "application/json")
    public List<UserAssessmentSummary> getLatestSummaryByCategory(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "assessmentCategoryGroupId") String groupId){

        Try<UserAssessmentSummary> userAssessmentSummaryMaybe = userAssessmentService.getLatestSummaryByGroup(determineUserId(userId), groupId);

        if (userAssessmentSummaryMaybe.isFailure() && userAssessmentSummaryMaybe.failed().get() instanceof RepoNotFoundException){
            throw new NotFoundException(userAssessmentSummaryMaybe.failed().get().getMessage());
        }

        UserAssessmentSummary userAssessmentSummary = userAssessmentSummaryMaybe.checkedGet();
        
        return userAssessmentSummary != null? [userAssessmentSummary] : [];
    }

}

