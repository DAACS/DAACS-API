package com.daacs.resource

import com.daacs.component.IPUtils
import com.daacs.model.ErrorResponse
import com.daacs.model.event.ErrorEvent
import com.daacs.service.EventService
import com.lambdista.util.Try
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletRequest
import javax.validation.Valid
/**
 * Created by chostetter on 7/5/16.
 */
@RestController
@RequestMapping(value = "/error-events", produces = "application/json")
public class ErrorEventController extends AuthenticatedController{

    @Autowired
    private EventService eventService;

    @Autowired
    private IPUtils ipUtils;

    @ApiOperation(
            value = "create a new error event"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Unable to create UserEvent"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.POST, produces = "application/json")
    public ErrorEvent createErrorEvent(@Valid @RequestBody ErrorEvent errorEvent,
                                       HttpServletRequest request){

        errorEvent.setIpAddress(ipUtils.getIPAddress(request));
        Try<Void> maybeRecorded = eventService.recordEvent(errorEvent);
        if(maybeRecorded.isFailure()){
            throw maybeRecorded.failed().get();
        }

        return errorEvent;
    }
}
