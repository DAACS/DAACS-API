package com.daacs.resource

import com.daacs.model.ErrorResponse
import com.daacs.model.dto.SendHelpEmailRequest
import com.daacs.service.MailService
import com.lambdista.util.Try
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

import javax.validation.Valid
/**
 * Created by chostetter on 7/12/16.
 */
@RestController
@RequestMapping(value = "/help", produces = "application/json")

class HelpController extends AuthenticatedController {

    @Autowired
    private MailService mailService


    @ApiOperation(
            value = "send a help email"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Unable to send email"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "", method = RequestMethod.POST, produces = "application/json")
    public void sendHelpEmail(@Valid @RequestBody SendHelpEmailRequest sendHelpEmailRequest){

        Try<Void> maybeSentEmail = mailService.sendHelpEmail(getLoggedInUser(), sendHelpEmailRequest.getAssessmentId(), sendHelpEmailRequest.getText(), sendHelpEmailRequest.getUserAgent())

        if(maybeSentEmail.isFailure()){
            throw maybeSentEmail.failed().get();
        }

    }
}

