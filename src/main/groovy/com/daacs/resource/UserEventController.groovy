package com.daacs.resource

import com.daacs.model.ErrorResponse
import com.daacs.model.event.UserEvent
import com.daacs.service.UserService
import com.lambdista.util.Try
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

import javax.validation.Valid
/**
 * Created by chostetter on 7/5/16.
 */
@RestController
@RequestMapping(value = "/user-events", produces = "application/json")
public class UserEventController extends AuthenticatedController{

    @Autowired
    private UserService userService;

    @ApiOperation(
            value = "create a new user event"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Unable to create UserEvent"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.POST, produces = "application/json")
    public UserEvent createUserEvent(@Valid @RequestBody UserEvent userEvent){

        Try<Void> maybeRecorded = userService.recordEvent(getLoggedInUser().getId(), userEvent);
        if(maybeRecorded.isFailure()){
            throw maybeRecorded.failed().get();
        }

        return userEvent;
    }
}
