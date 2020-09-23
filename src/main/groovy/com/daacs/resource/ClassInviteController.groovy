package com.daacs.resource

import com.daacs.model.ErrorResponse
import com.daacs.model.InstructorClass
import com.daacs.model.dto.AcceptClassInviteRequest
import com.daacs.model.dto.SendClassInviteRequest
import com.daacs.service.InstructorClassService
import com.daacs.service.UserService
import com.lambdista.util.Try
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

import javax.validation.Valid

/**
 * Created by mgoldman
 */
@RestController
@RequestMapping(value = "/class-invite", produces = "application/json")
public class ClassInviteController extends AuthenticatedController {
    protected static final Logger log = LoggerFactory.getLogger(ClassInviteController.class);

    @Autowired
    private InstructorClassService instructorClassService;

    @ApiOperation(
            value = "send class invites to students"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "send", method = RequestMethod.POST, produces = "application/json")
    public void sendClassInvite(@Valid @RequestBody SendClassInviteRequest classInviteRequest) {

        checkPermissions([ROLE_INSTRUCTOR, ROLE_ADMIN]);

        instructorClassService.sendClassInvites(classInviteRequest).checkedGet();
    }

    @ApiOperation(
            value = "accept a class invite"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Unable to accept class invite"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "accept", method = RequestMethod.PUT, produces = "application/json")
    public void acceptInvite(@Valid @RequestBody AcceptClassInviteRequest acceptInviteRequest) {
        instructorClassService.acceptInvite(acceptInviteRequest.getClassId(), acceptInviteRequest.getUserId()).checkedGet();
    }
}
