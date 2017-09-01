package com.daacs.resource

import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.AssessmentCategory
import com.daacs.model.assessment.AssessmentCategorySummary
import com.daacs.service.AssessmentService
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
@RequestMapping(value = "/assessment-category-summaries", produces = "application/json")
public class AssessmentCategorySummaryController extends AuthenticatedController{

    @Autowired
    private AssessmentService assessmentService;

    @ApiOperation(
            value = "list all assessment category summaries for the current user",
            response = AssessmentCategorySummary,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    public List<AssessmentCategorySummary> getSummaries(
            @RequestParam(value = "assessmentCategory[]", required = false) List<AssessmentCategory> assessmentCategories,
            @RequestParam(value = "userId", required = false) String userId){

        if(assessmentCategories == null){
            assessmentCategories = AssessmentCategory.values();
        }

        return assessmentService.getCategorySummaries(determineUserId(userId), assessmentCategories).checkedGet();
    }

}
