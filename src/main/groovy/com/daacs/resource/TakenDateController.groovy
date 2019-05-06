package com.daacs.resource

import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.AssessmentCategory
import com.daacs.model.assessment.user.UserAssessmentTakenDate
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
/**
 * Created by chostetter on 7/12/16.
 */
@RestController
@RequestMapping(value = "/user-assessment-taken-dates", produces = "application/json")

class TakenDateController extends AuthenticatedController{

    @Autowired
    UserAssessmentService userAssessmentService;

    @ApiOperation(
            value = "list dates taken for a user assessment",
            response = Map,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid userId or assessmentCategory"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.GET, params = ["assessmentCategoryGroupId"], produces = "application/json")
    public List<UserAssessmentTakenDate> getTakenDatesByCategory(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "assessmentCategoryGroupId") String groupId){

        Try<List<UserAssessmentTakenDate>> maybeTakenDates = userAssessmentService.getTakenDates(determineUserId(userId), groupId);
        if(maybeTakenDates.isFailure()){
            throw maybeTakenDates.failed().get();
        }

        return maybeTakenDates.get();
    }

}

