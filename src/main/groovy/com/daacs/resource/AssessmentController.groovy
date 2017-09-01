package com.daacs.resource

import com.daacs.framework.exception.BadInputException
import com.daacs.framework.validation.annotations.group.CreateGroup
import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.AssessmentCategory
import com.daacs.model.dto.UpdateAssessmentRequest
import com.daacs.model.dto.UpdateLightSideModelsRequest
import com.daacs.service.AssessmentService
import com.lambdista.util.Try
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.apache.commons.fileupload.FileItemIterator
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

import javax.servlet.http.HttpServletRequest
import javax.validation.Valid
/**
 * Created by chostetter on 7/5/16.
 */
@RestController
@RequestMapping(value = "/assessments", produces = "application/json")
public class AssessmentController extends AuthenticatedController{
    protected static final Logger log = LoggerFactory.getLogger(AssessmentController.class);

    @Autowired
    private AssessmentService assessmentService;


    @ApiOperation(
            value = "reload assessments"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Unable to create Assessment"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "/reload", method = RequestMethod.POST, produces = "application/json")
    public void reloadDummyAssessments(){

        checkPermissions([ROLE_ADMIN]);

        Try<Void> maybeResult = assessmentService.reloadDummyAssessments();
        if(maybeResult.isFailure()){
            throw maybeResult.failed().get();
        }
    }

    @ApiOperation(
            value = "list assessments",
            response = Assessment,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    public List<Assessment> getAssessments(){

        checkPermissions([ROLE_ADMIN]);

        Try<List<Assessment>> maybeAssessments = assessmentService.getAssessments(null, Arrays.asList(AssessmentCategory.class.getEnumConstants()));
        if(maybeAssessments.isFailure()){
            throw maybeAssessments.failed().get();
        }

        return maybeAssessments.get();
    }

    @ApiOperation(
            value = "create a new assessment"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.POST, produces = "application/json")
    public Assessment createAssessment(@Validated(CreateGroup.class) @RequestBody Assessment assessment){

        checkPermissions([ROLE_ADMIN]);

        Try<Assessment> maybeAssessment = assessmentService.createAssessment(assessment);
        if(maybeAssessment.isFailure()){
            throw maybeAssessment.failed().get();
        }

        return maybeAssessment.get();
    }

    @ApiOperation(
            value = "update an assessment"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, produces = "application/json")
    public Assessment updateAssessment(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateAssessmentRequest updateAssessmentRequest){

        checkPermissions([ROLE_ADMIN]);

        if(updateAssessmentRequest.getId() != id) {
            throw new BadInputException("Assessment", "ID in the body and the ID passed into the URL do not match");
        }

        Try<Assessment> maybeAssessment = assessmentService.updateAssessment(updateAssessmentRequest);
        if(maybeAssessment.isFailure()){
            throw maybeAssessment.failed().get();
        }

        return maybeAssessment.get();
    }

    @ApiOperation(
            value = "upload lightside configuration files"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/{id}/upload-lightside-models", method = RequestMethod.POST, produces = "application/json")
    public void uploadLightSideModels(
            @PathVariable("id") String id,
            HttpServletRequest request) {

        checkPermissions([ROLE_ADMIN]);

        //is this a valid upload?
        if(!ServletFileUpload.isMultipartContent(request)){
            throw new Exception("Request must be multipart/form-data");
        }

        ServletFileUpload upload = new ServletFileUpload();
        FileItemIterator fileItemIterator = upload.getItemIterator(request);

        UpdateLightSideModelsRequest requestModel = new UpdateLightSideModelsRequest(
                assessmentId: id,
                fileItemIterator: fileItemIterator);

        Try<Assessment> maybeUpdate = assessmentService.updateWritingAssessment(requestModel);
        if(maybeUpdate.isFailure()){
            throw maybeUpdate.failed().get()
        }
    }

    @ApiOperation(
            value = "get an assessment",
            response = Assessment
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid userId"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
    public Assessment getAssessment(
            @PathVariable("id") String id){

        checkPermissions([ROLE_ADMIN]);

        Try<Assessment> maybeAssessment = assessmentService.getAssessment(id);
        if(maybeAssessment.isFailure()){
            throw maybeAssessment.failed().get();
        }

        return maybeAssessment.get();

    }
}
