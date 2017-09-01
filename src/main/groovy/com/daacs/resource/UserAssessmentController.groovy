package com.daacs.resource

import com.daacs.framework.exception.BadInputException
import com.daacs.framework.serializer.Views
import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.user.UserAssessment
import com.daacs.model.dto.CreateUserAssessmentRequest
import com.daacs.model.dto.UpdateUserAssessmentRequest
import com.daacs.service.UserAssessmentService
import com.fasterxml.jackson.annotation.JsonView
import com.lambdista.util.Try
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

import javax.validation.Valid
/**
 * Created by chostetter on 7/5/16.
 */
@RestController
@RequestMapping(value = "/user-assessments", produces = "application/json")
public class UserAssessmentController extends AuthenticatedController {

    @Autowired
    private UserAssessmentService userAssessmentService;

    @ApiOperation(
            value = "create a new user assessment"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Unable to create UserAssessment"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.POST, produces = "application/json")
    public UserAssessment createUserAssessment(@Valid @RequestBody CreateUserAssessmentRequest createUserAssessmentRequest){
        String assessmentId = createUserAssessmentRequest.getAssessmentId();

        Try<UserAssessment> maybeUserAssessment = userAssessmentService.createUserAssessment(getLoggedInUser(), assessmentId);
        if(maybeUserAssessment.isFailure()){
            throw maybeUserAssessment.failed().get();
        }

        return maybeUserAssessment.get();
    }

    @ApiOperation(
            value = "update a new user assessment"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Unable to update UserAssessment"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @JsonView(Views.Student)
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, produces = "application/json")
    public UserAssessment updateUserAssessment(
            @PathVariable("id") String id,
            @RequestParam(value = "userId", required = false) String userId,
            @Valid @RequestBody UpdateUserAssessmentRequest updateUserAssessmentRequest){

        if(updateUserAssessmentRequest.getId() != id) {
            throw new BadInputException("UserAssessment", "ID in the body and the ID passed into the URL do not match");
        }

        Try<UserAssessment> maybeUserAssessment = userAssessmentService.updateUserAssessment(determineUserId(userId), getLoggedInUser().getRoles(), updateUserAssessmentRequest);
        if(maybeUserAssessment.isFailure()){
            throw maybeUserAssessment.failed().get();
        }

        return maybeUserAssessment.get();
    }

    @ApiOperation(
            value = "get a user assessment",
            response = UserAssessment
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid userId"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @JsonView(Views.Student)
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, params = ["!userId"], produces = "application/json")
    public UserAssessment getUserAssessment(
            @PathVariable("id") String id){

        Try<UserAssessment> maybeUserAssessment = userAssessmentService.getUserAssesment(getLoggedInUser().getId(), id);
        if(maybeUserAssessment.isFailure()){
            throw maybeUserAssessment.failed().get();
        }

        return maybeUserAssessment.get();

    }

    @ApiOperation(
            value = "get a user assessment",
            response = UserAssessment
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid userId"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @JsonView(Views.Admin)
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, params = ["userId"], produces = "application/json")
    public UserAssessment getUserAssessment(
            @PathVariable("id") String id,
            @RequestParam(value = "userId") String userId){

        checkPermissions([ROLE_ADMIN]);

        Try<UserAssessment> maybeUserAssessment = userAssessmentService.getUserAssesment(userId, id);
        if(maybeUserAssessment.isFailure()){
            throw maybeUserAssessment.failed().get();
        }

        return maybeUserAssessment.get();

    }
}
