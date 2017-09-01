package com.daacs.resource

import com.daacs.framework.exception.BadInputException
import com.daacs.framework.serializer.Views
import com.daacs.model.ErrorResponse
import com.daacs.model.dto.SaveItemGroupRequest
import com.daacs.model.dto.SaveWritingSampleRequest
import com.daacs.model.item.ItemGroup
import com.daacs.model.item.WritingPrompt
import com.daacs.service.ItemGroupService
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
 * Created by chostetter on 7/12/16.
 */
@RestController
@RequestMapping(value = "", produces = "application/json")

class QuestionController extends AuthenticatedController {

    @Autowired
    private ItemGroupService itemGroupService;

    @Autowired
    private UserAssessmentService userAssessmentService;

    @ApiOperation(
            value = "returns next question item group for assessment",
            response = ItemGroup,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid assessmentId"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @JsonView(Views.Student)
    @RequestMapping(value = "/user-assessment-question-groups", method = RequestMethod.GET, params = ["assessmentId", "limit=1"], produces = "application/json")
    public List<ItemGroup> getNextItemGroup(
            @RequestParam(value = "assessmentId") String assessmentId){

        Try<ItemGroup> maybeItemGroup = itemGroupService.getNextItemGroup(getLoggedInUser().getId(), assessmentId);
        if(maybeItemGroup.isFailure()){
            throw maybeItemGroup.failed().get();
        }

        if(maybeItemGroup.toOptional().isPresent()){
            return [maybeItemGroup.get()];
        }

        return [];
    }

    @ApiOperation(
            value = "returns next question item group for assessment",
            response = ItemGroup,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid assessmentId"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @JsonView(Views.Student)
    @RequestMapping(value = "/user-assessment-writing-samples", method = RequestMethod.GET, params = ["assessmentId", "limit=1"], produces = "application/json")
    public List<WritingPrompt> getWritingPrompt(
            @RequestParam(value = "assessmentId") String assessmentId){

        Try<WritingPrompt> maybeWritingPrompt = userAssessmentService.getWritingPrompt(getLoggedInUser().getId(), assessmentId);
        if(maybeWritingPrompt.isFailure()){
            throw maybeWritingPrompt.failed().get();
        }

        if(maybeWritingPrompt.toOptional().isPresent()){
            return [maybeWritingPrompt.get()];
        }

        return [];
    }

    @ApiOperation(
            value = "update a question group on a user assessment (post answers)"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @JsonView(Views.Student)
    @RequestMapping(value = "/user-assessment-question-groups/{id}", method = RequestMethod.PUT, produces = "application/json")
    public ItemGroup updateItemGroup(
            @PathVariable("id") String id,
            @Valid @RequestBody SaveItemGroupRequest saveItemGroupRequest){

        if(saveItemGroupRequest.getId() != id){
            throw new BadInputException("ItemGroup", "ID in the body and the ID passed into the URL do not match.")
        }

        Try<ItemGroup> maybeItemGroup = itemGroupService.saveItemGroup(getLoggedInUser().getId(), saveItemGroupRequest.getAssessmentId(), saveItemGroupRequest);

        if(maybeItemGroup.isFailure()){
            throw maybeItemGroup.failed().get();
        }

        return maybeItemGroup.get();
    }

    @ApiOperation(
            value = "save a writing sample on a user assessment (post answers)"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/user-assessment-writing-samples/{id}", method = RequestMethod.PUT, produces = "application/json")
    public WritingPrompt saveWritingSample(
            @PathVariable("id") String id,
            @Valid @RequestBody SaveWritingSampleRequest saveWritingSampleRequest){

        if(saveWritingSampleRequest.getId() != id){
            throw new BadInputException("SaveWritingSampleRequest", "ID in the body and the ID passed into the URL do not match.")
        }

        Try<WritingPrompt> maybeWritingSample = userAssessmentService.saveWritingSample(getLoggedInUser().getId(), saveWritingSampleRequest.getAssessmentId(), saveWritingSampleRequest);
        if(maybeWritingSample.isFailure()){
            throw maybeWritingSample.failed().get();
        }

        return maybeWritingSample.get();
    }


}

