package com.daacs.resource

import com.daacs.framework.exception.BadInputException
import com.daacs.model.ErrorResponse
import com.daacs.model.InstructorClass

import com.daacs.service.InstructorClassService
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

import javax.validation.Valid

/**
 * Created by mgoldman on 6/8/20.
 */
@RestController
@RequestMapping(value = "/classes", produces = "application/json")
public class InstructorClassController extends AuthenticatedController {
    protected static final Logger log = LoggerFactory.getLogger(InstructorClassController.class);

    @Autowired
    private InstructorClassService instructorClassService;


    @ApiOperation(
            value = "list classes",
            response = InstructorClass,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    public List<InstructorClass> getInstructorClasses(
            @RequestParam(value = "instructorId", required = false, defaultValue = '') String[] instructorIds,
            @RequestParam(value = "limit", required = false, defaultValue = '0') String limit,
            @RequestParam(value = "offset", required = false, defaultValue = '0') String offset) {

        Try<List<InstructorClass>> maybeInstructorClass = instructorClassService.getClasses(instructorIds, Integer.parseInt(limit), Integer.parseInt(offset));
        if (maybeInstructorClass.isFailure()) {
            throw maybeInstructorClass.failed().get();
        }

        return maybeInstructorClass.get();
    }

    @ApiOperation(
            value = "create a new instructorClass"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.POST, produces = "application/json")
    public InstructorClass createInstructorClass(@Valid @RequestBody InstructorClass instructorClass) {

        checkPermissions([ROLE_INSTRUCTOR, ROLE_ADMIN]);

        Try<InstructorClass> maybeInstructorClass = instructorClassService.createClass(instructorClass);
        InstructorClass instructorClassResponse = maybeInstructorClass.checkedGet()

        return instructorClassResponse
    }

    @ApiOperation(
            value = "update an instructorClass"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, produces = "application/json")
    public InstructorClass updateInstructorClass(
            @PathVariable("id") String id,
            @Valid @RequestBody InstructorClass updateInstructorClassRequest) {

        checkPermissions([ROLE_INSTRUCTOR, ROLE_ADMIN]);

        if (updateInstructorClassRequest.getId() != id) {
            throw new BadInputException("InstructorClass", "ID in the body and the ID passed into the URL do not match");
        }

        Try<InstructorClass> maybeInstructorClass = instructorClassService.updateClass(updateInstructorClassRequest);
        InstructorClass instructorClassResponse = maybeInstructorClass.checkedGet()

        return instructorClassResponse;
    }

    @ApiOperation(
            value = "get an instructorClass",
            response = InstructorClass
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid userId"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
    public InstructorClass getInstructorClass(
            @PathVariable("id") String id) {

        Try<InstructorClass> maybeInstructorClass = instructorClassService.getClass(id);
        if (maybeInstructorClass.isFailure()) {
            throw maybeInstructorClass.failed().get();
        }

        return maybeInstructorClass.get();

    }


    @ApiOperation(
            value = "upload a csv of classes",
            response = URL
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = URL, message = "Unable to add classes"),
            @ApiResponse(code = 500, response = URL, message = "Unknown error"),
            @ApiResponse(code = 503, response = URL, message = "Retryable error")
    ])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "/upload", method = RequestMethod.POST, produces = "application/json")
    public void uploadClasses(@RequestParam(value = "file", required = true) MultipartFile file) {

        instructorClassService.uploadClasses(file).checkedGet()
    }
}
