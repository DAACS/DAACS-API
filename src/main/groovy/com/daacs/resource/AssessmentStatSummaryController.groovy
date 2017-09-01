package com.daacs.resource

import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.AssessmentStatSummary
import com.daacs.service.AssessmentService
import com.lambdista.util.Try
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

/**
 * Created by adistasio on 10/13/16.
 */
@RestController
@RequestMapping(value = "/assessment-stat-summaries", produces = "application/json")
public class AssessmentStatSummaryController extends AuthenticatedController{

    @Autowired
    private AssessmentService assessmentService;


    @ApiOperation(
            value = "list assessment stats",
            response = AssessmentStatSummary,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    public List<AssessmentStatSummary> getAssessmentStats(){

        checkPermissions([ROLE_ADMIN]);

        Try<List<AssessmentStatSummary>> maybeAssessments = assessmentService.getAssessmentStats();
        if(maybeAssessments.isFailure()){
            throw maybeAssessments.failed().get();
        }

        return maybeAssessments.get();
    }
}
