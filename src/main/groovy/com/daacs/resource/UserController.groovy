package com.daacs.resource

import com.daacs.framework.auth.service.SessionService
import com.daacs.framework.exception.BadInputException
import com.daacs.framework.exception.EndpointDisabledException
import com.daacs.framework.exception.InsufficientPermissionsException
import com.daacs.model.ErrorResponse
import com.daacs.model.SessionedUser
import com.daacs.model.User
import com.daacs.model.UserSearchResult
import com.daacs.model.dto.CreateUserRequest
import com.daacs.model.dto.UpdateUserRequest
import com.daacs.service.UserAssessmentService
import com.daacs.service.UserService
import com.lambdista.util.Try
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*

import javax.validation.Valid

/**
 * Created by chostetter on 7/5/16.
 */
@RestController
@RequestMapping(value = "/users", produces = "application/json")
public class UserController extends AuthenticatedController{

    @Value('${user.createEnabled}')
    private boolean createAccountEnabled;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserAssessmentService userAssessmentService;

    @Autowired
    private UserService userService;

    @ApiOperation(
            value = "get logged in user",
            response = SessionedUser
    )
    @ApiResponses(value = [
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/me", method = RequestMethod.GET, produces = "application/json")
    public SessionedUser getMe(){
        return sessionService.getRequiredSessionedUser();
    }

    @ApiOperation(
            value = "get user by id",
            response = SessionedUser
    )
    @ApiResponses(value = [
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
    public SessionedUser getSessionedUser(@PathVariable("id") String id){

        User user = getLoggedInUser();
        if(user.getId() != id && !hasRole(user, [ROLE_ADMIN, ROLE_ADVISOR])){
            throw new InsufficientPermissionsException("user");
        }

        return new SessionedUser(userService.getUser(id).checkedGet());
    }

    @ApiOperation(
            value = "update a user"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Unable to update User"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = ["/{id}"], method = RequestMethod.PUT, produces = "application/json")
    public SessionedUser updateUser(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateUserRequest updateUserRequest){

        if(updateUserRequest.getId() != id) {
            throw new BadInputException("User", "ID in the body and the ID passed into the URL do not match");
        }

        updateUserRequest.setId(getLoggedInUser().getId());
        return new SessionedUser(userService.updateUser(updateUserRequest).checkedGet());
    }

    @ApiOperation(
            value = "create a user"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Unable to create User"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = [""], method = RequestMethod.POST, produces = "application/json")
    public SessionedUser createUser(
            @Valid @RequestBody CreateUserRequest createUserRequest){

        if (!createAccountEnabled) {
            throw new EndpointDisabledException("The create user endpoint has been disabled by configuration")
        }

        return new SessionedUser(userService.createUser(createUserRequest).checkedGet());
    }


    @ApiOperation(
            value = "search for users who have taken an assessment",
            response = UserSearchResult,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.GET, params = ["keywords"], produces = "application/json")
    public List<UserSearchResult> searchUsers(
            @RequestParam(value = "keywords") String keywordString){

        checkPermissions([ROLE_ADMIN, ROLE_ADVISOR, ROLE_INSTRUCTOR]);

        keywordString = URLDecoder.decode(keywordString, "UTF-8");
        Try<List<UserSearchResult>> maybeSearchResults = userService.searchUsers(Arrays.asList(keywordString.split(" "),), null, 10);
        if(maybeSearchResults.isFailure()){
            throw maybeSearchResults.failed().get();
        }

        return maybeSearchResults.get();
    }

    @ApiOperation(
            value = "search for users by role",
            response = UserSearchResult,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.GET, params = ["role", "keywords"], produces = "application/json")
    public List<UserSearchResult> searchUsersByRole(
            @RequestParam(value = "role") String roleString,
            @RequestParam(value = "keywords") String keywordString){

        checkPermissions([ROLE_ADMIN, ROLE_ADVISOR, ROLE_INSTRUCTOR]);

        keywordString = URLDecoder.decode(keywordString, "UTF-8");
        Try<List<UserSearchResult>> maybeSearchResults = userService.searchUsers(Arrays.asList(keywordString.split(" ")), roleString, 10);
        if(maybeSearchResults.isFailure()){
            throw maybeSearchResults.failed().get();
        }

        return maybeSearchResults.get();
    }

    @ApiOperation(
            value = "get user by username",
            response = User,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.GET, params = ["username"], produces = "application/json")
    public List<User> getUserByUsername(
            @RequestParam(value = "username") String username){

        checkPermissions([ROLE_ADMIN, ROLE_ADVISOR]);

        Try<User> maybeUser = userService.getUserByUsername(username);
        if(maybeUser.isFailure()){
            throw maybeUser.failed().get();
        }

        return [maybeUser.get()];
    }

    @ApiOperation(
            value = "get user by secondaryId",
            response = User,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.GET, params = ["secondaryId"], produces = "application/json")
    public List<User> getUserBySecondaryId(
            @RequestParam(value = "secondaryId") String secondaryId){

        checkPermissions([ROLE_ADMIN, ROLE_ADVISOR]);

        Try<User> maybeUser = userService.getUserBySecondaryId(secondaryId);
        if(maybeUser.isFailure()){
            throw maybeUser.failed().get();
        }

        return [maybeUser.get()];
    }
}
