package com.daacs.resource

import com.daacs.model.ErrorResponse
import com.daacs.model.dto.ForgotPasswordRequest
import com.daacs.model.dto.ResetPasswordRequest
import com.daacs.service.MailService
import com.daacs.service.UserService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

import javax.validation.Valid
/**
 * Created by chostetter on 3/2/17.
 */
@RestController
@RequestMapping(value = "/forgot-password", produces = "application/json")
public class ForgotPasswordController extends AuthenticatedController {

    @Autowired
    private MailService mailService;

    @Autowired
    private UserService userService;

    @ApiOperation(
            value = "request a forgot-password email"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Unable to reset password"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "", method = RequestMethod.POST, produces = "application/json")
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest){
        mailService.sendForgotPasswordEmail(forgotPasswordRequest.getUsername()).checkedGet();
    }

    @ApiOperation(
            value = "reset a user's password"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Unable to reset password"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "", method = RequestMethod.PUT, produces = "application/json")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest){
        userService.resetPassword(resetPasswordRequest.getUserId(), resetPasswordRequest.getPassword(), resetPasswordRequest.getCode()).checkedGet();
    }
}
