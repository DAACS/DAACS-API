package com.daacs.resource

import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.AssessmentCategory
import com.daacs.model.assessment.AssessmentContent
import com.daacs.service.AssessmentService
import com.lambdista.util.Try
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

import java.time.Instant

/**
 * Created by chostetter on 7/5/16.
 */
@RestController
@RequestMapping(value = "/assessment-contents", produces = "application/json")
public class AssessmentContentController extends AuthenticatedController{

    @Autowired
    private AssessmentService assessmentService;

    @ApiOperation(
            value = "get assessment content",
            response = AssessmentContent
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
    public AssessmentContent getContent(
            @PathVariable("id") String id){
        return assessmentService.getContent(id).checkedGet();
    }

    @ApiOperation(
            value = "get enabled assessment content by category",
            response = AssessmentContent
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.GET, params = ["assessmentCategoryGroupId"], produces = "application/json")
    public AssessmentContent getContentByCategory(
            @RequestParam(value = "assessmentCategoryGroupId") String groupId,
            @RequestParam(value = "takenDate", required = false) String takenDate,
            @RequestParam(value = "userId", required = false) String userId){

        Try<AssessmentContent> maybeAssessmentContent;
        if(takenDate == null){
            maybeAssessmentContent = assessmentService.getContentByCategoryGroup(groupId);
        }
        else{
            maybeAssessmentContent = assessmentService.getContentForUserAssessment(determineUserId(userId), groupId, Instant.parse(takenDate));
        }

        return maybeAssessmentContent.checkedGet();
    }
}
