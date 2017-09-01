package com.daacs.resource

import com.daacs.framework.core.ThreadLocalTrackingData
import com.daacs.model.ErrorResponse
import com.daacs.model.event.EventType
import com.daacs.model.event.UserEvent
import com.daacs.service.UserService
import com.lambdista.util.Try
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletRequest
/**
 * Created by chostetter on 12/15/16.
 */
@RestController
@RequestMapping(value = "/oauth", produces = "application/json")
public class OAuthController extends AuthenticatedController {
    private static final Logger log = LoggerFactory.getLogger(OAuthController.class);

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private UserService userService;

    @ApiOperation(
            value = "remove token"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = ["/remove-token"], method = RequestMethod.GET, produces = "application/json")
    public void removeToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            String userId = getLoggedInUser().getId();

            String tokenValue = authHeader.replace("Bearer", "").trim();
            OAuth2AccessToken accessToken = tokenStore.readAccessToken(tokenValue);
            tokenStore.removeAccessToken(accessToken);

            Try<Void> maybeRecorded = userService.recordEvent(userId, new UserEvent(EventType.LOGOUT, ThreadLocalTrackingData.getTrackingData()));
            if(maybeRecorded.isFailure()){
                Throwable t = maybeRecorded.failed().get();
                log.error(t.getMessage(), t);
            }
        }
    }
}
