package com.daacs.resource

import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.AssessmentCategoryGroup
import com.daacs.service.AssessmentCategoryGroupService
import com.lambdista.util.Try
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

import javax.validation.Valid

/**
 * Created by mgoldman on 2/28/19.
 */
@RestController
@RequestMapping(value = "/assessment-category-groups", produces = "application/json")
public class AssessmentCategoryGroupController extends AuthenticatedController {

    @Autowired
    private AssessmentCategoryGroupService groupService;

    @ApiOperation(
            value = "list all assessment category groups",
            response = AssessmentCategoryGroup,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    public List<AssessmentCategoryGroup> getGroups() {
        checkPermissions([ROLE_ADMIN]);
        return groupService.getCategoryGroups().checkedGet()
    }

    @ApiOperation(
            value = "create a new assessment category group"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Unable to create assessment category group"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.POST, produces = "application/json")
    public AssessmentCategoryGroup CreateAssessmentCategoryGroup(@Valid @RequestBody AssessmentCategoryGroup categoryGroup) {

        checkPermissions([ROLE_ADMIN])
        Try<AssessmentCategoryGroup> maybeGroup = groupService.createCategoryGroup(categoryGroup);

        return maybeGroup.checkedGet()
    }

    @ApiOperation(
            value = "update a assessment category group"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Unable to update assessment category group"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = ["/{id}"], method = RequestMethod.PUT, produces = "application/json")
    public AssessmentCategoryGroup UpdateAssessmentCategoryGroup(@PathVariable("id") String id,
                                                                 @Valid @RequestBody AssessmentCategoryGroup categoryGroup) {
        checkPermissions([ROLE_ADMIN])
        Try<AssessmentCategoryGroup> maybeGroup = groupService.updateCategoryGroup(id, categoryGroup);

        return maybeGroup.checkedGet()
    }

    @ApiOperation(
            value = "delete a assessment category group"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Unable to delete assessment category group"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = ["/{id}"], method = RequestMethod.DELETE, produces = "application/json")
    public void DeleteAssessmentCategoryGroup(@PathVariable("id") String id) {
        checkPermissions([ROLE_ADMIN])
        Try<Void> maybeDeleted = groupService.deleteCategoryGroup(id)
        if (maybeDeleted.isFailure()) {
            throw maybeDeleted.failed().get()
        }

    }

}
