package com.daacs.resource

import com.daacs.framework.exception.BadInputException
import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.AssessmentCategory
import com.daacs.model.dto.AssessmentResponse
import com.daacs.model.dto.UpdateAssessmentRequest
import com.daacs.model.dto.response.LightSideModelFileNameResponse
import com.daacs.service.AssessmentService
import com.lambdista.util.Try
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile


/**
 * Created by chostetter on 7/5/16.
 */
@RestController
@RequestMapping(value = "/assessments", produces = "application/json")
public class AssessmentController extends AuthenticatedController {
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
    public void reloadDummyAssessments() {

        checkPermissions([ROLE_ADMIN]);

        Try<Void> maybeResult = assessmentService.reloadDummyAssessments();
        if (maybeResult.isFailure()) {
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
    public List<Assessment> getAssessments() {

        checkPermissions([ROLE_ADMIN]);

        Try<List<Assessment>> maybeAssessments = assessmentService.getAssessments(null, null);
        if (maybeAssessments.isFailure()) {
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
    public Assessment createAssessment(@RequestBody Assessment assessment) {

        checkPermissions([ROLE_ADMIN]);

        Try<AssessmentResponse> maybeAssessment = assessmentService.createAssessment(assessment);
        AssessmentResponse assessmentResponse = maybeAssessment.checkedGet()

        assessmentResponse.getData().setErrors(assessmentResponse.getMeta())
        return assessmentResponse.data;
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
            @RequestBody UpdateAssessmentRequest updateAssessmentRequest) {

        checkPermissions([ROLE_ADMIN]);

        if (updateAssessmentRequest.getId() != id) {
            throw new BadInputException("Assessment", "ID in the body and the ID passed into the URL do not match");
        }

        Try<AssessmentResponse> maybeAssessment = assessmentService.updateAssessment(updateAssessmentRequest);
        AssessmentResponse assessmentResponse = maybeAssessment.checkedGet()

        assessmentResponse.getData().setErrors(assessmentResponse.getMeta())
        return assessmentResponse.data;
    }

    @ApiOperation(
            value = "upload lightside configuration files"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/upload-lightside-models", method = RequestMethod.POST, produces = "application/json")
    public LightSideModelFileNameResponse uploadLightSideModels(@RequestParam(value = "file", required = true) MultipartFile file) {

        checkPermissions([ROLE_ADMIN]);

        Try<String> maybeFileName = assessmentService.uploadLightSideModel(file)
        if (maybeFileName.isFailure()) {
            throw maybeFileName.failed().get()
        }
        return new LightSideModelFileNameResponse(maybeFileName.get())
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
            @PathVariable("id") String id) {

        checkPermissions([ROLE_ADMIN]);

        Try<Assessment> maybeAssessment = assessmentService.getAssessment(id);
        if (maybeAssessment.isFailure()) {
            throw maybeAssessment.failed().get();
        }

        return maybeAssessment.get();

    }
}
