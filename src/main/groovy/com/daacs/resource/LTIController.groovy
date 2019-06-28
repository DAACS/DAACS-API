package com.daacs.resource

import com.daacs.framework.exception.EndpointDisabledException
import com.daacs.model.ErrorResponse

import com.daacs.service.LtiService
import com.lambdista.util.Try
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.imsglobal.aspect.Lti
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView

import javax.servlet.http.HttpServletRequest

/**
 * Created by mgoldman on 2/26/19.
 */
@RestController
@RequestMapping(value = "/lti", produces = "application/json")
public class LTIController extends AuthenticatedController {
    @Value('${lti.frontendAuthSuccessUrl}')
    private String successUrl

    @Value('${lti.enabled}')
    private boolean ltiEnabled

    @Autowired
    private LtiService ltiService;

    @ApiOperation(
            value = "login through LTI",
            response = RedirectView
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid LTI request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown Error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @Lti
    @RequestMapping(value = "", method = RequestMethod.POST, produces = "application/json")
    public RedirectView ltiLaunch(HttpServletRequest ltiLaunchRequest) {

        //this endpoint shouldn't be usable on non-lti configured instances
        if (!ltiEnabled) {
            throw new EndpointDisabledException("The LTI login endpoint has been disabled by configuration")
        }

        Try<String> maybeResponse = ltiService.verifyLaunch(ltiLaunchRequest)

        return new RedirectView(successUrl + "?token="+maybeResponse.checkedGet())

    }

    @ApiOperation(
            value = "update grades through LTI",
            response = RedirectView
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid LTI request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown Error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @Lti
    @RequestMapping(value = "/update-grades", method = RequestMethod.POST, produces = "application/json")
    public RedirectView ltiUpdateGrades(HttpServletRequest ltiLaunchRequest) {

        //this endpoint shouldn't be usable on non-lti configured instances
        if (!ltiEnabled) {
            throw new EndpointDisabledException("The LTI endpoint has been disabled by configuration")
        }

        Try<String> maybeResponse = ltiService.verifyLaunch(ltiLaunchRequest)

        return new RedirectView(successUrl + "?token="+maybeResponse.checkedGet())

    }

}

