package com.daacs.resource

import com.daacs.framework.serializer.Views
import com.daacs.model.ErrorResponse
import com.daacs.model.item.ItemGroup
import com.daacs.model.item.WritingPrompt
import com.daacs.service.UserAssessmentService
import com.fasterxml.jackson.annotation.JsonView
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
@RequestMapping(value = "", produces = "application/json")

class AnswerController extends AuthenticatedController {

    @Autowired
    private UserAssessmentService userAssessmentService;


    @ApiOperation(
            value = "list user assessment answers",
            response = ItemGroup,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid userId, assessmentId, or takenDate"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @JsonView(Views.CompletedAssessment)
    @RequestMapping(value = "/user-assessment-question-answers", method = RequestMethod.GET, params = ["assessmentId", "domainId", "takenDate"], produces = "application/json")
    public List<ItemGroup> getAnswers(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "assessmentId") String assessmentId,
            @RequestParam(value = "domainId") String domainId,
            @RequestParam(value = "takenDate") String takenDate){

        Try<List<ItemGroup>> maybeItemGroups = userAssessmentService.getAnswers(determineUserId(userId), assessmentId, domainId, Instant.parse(takenDate));
        if(maybeItemGroups.isFailure()){
            throw maybeItemGroups.failed().get();
        }

        return maybeItemGroups.get();
    }

    @ApiOperation(
            value = "list user assessment writing sample answers",
            response = String,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid userId, assessmentId, or takenDate"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/user-assessment-writing-sample-answers", method = RequestMethod.GET, params = ["assessmentId", "takenDate"], produces = "application/json")
    public List<WritingPrompt> getWritingSamples(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "assessmentId") String assessmentId,
            @RequestParam(value = "takenDate") String takenDate){

        Try<WritingPrompt> maybeWritingSample = userAssessmentService.getWritingSample(determineUserId(userId), assessmentId, Instant.parse(takenDate));
        if(maybeWritingSample.isFailure()){
            throw maybeWritingSample.failed().get();
        }

        return [maybeWritingSample.get()];
    }
}

