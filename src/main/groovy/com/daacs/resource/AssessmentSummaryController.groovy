package com.daacs.resource

import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.AssessmentCategory
import com.daacs.model.assessment.AssessmentSummary
import com.daacs.service.AssessmentService
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
 * Created by chostetter on 7/5/16.
 */
@RestController
@RequestMapping(value = "/assessment-summaries", produces = "application/json")
public class AssessmentSummaryController extends AuthenticatedController{

    @Autowired
    private AssessmentService assessmentService;

    @ApiOperation(
            value = "list all assessment summaries for the current user",
            response = AssessmentSummary,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    public List<AssessmentSummary> getSummaries(
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            @RequestParam(value = "userId", required = false) String userId){

        Try<List<AssessmentSummary>> maybeAssessmentSummaries = assessmentService.getSummaries(determineUserId(userId), enabled, Arrays.asList(AssessmentCategory.class.getEnumConstants()));
        if(maybeAssessmentSummaries.isFailure()){
            throw maybeAssessmentSummaries.failed().get();
        }

        return maybeAssessmentSummaries.get();
    }

}
