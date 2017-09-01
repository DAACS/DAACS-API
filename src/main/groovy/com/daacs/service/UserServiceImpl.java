package com.daacs.service;

import com.daacs.framework.exception.RepoNotFoundException;
import com.daacs.framework.hystrix.FailureTypeException;
import com.daacs.framework.serializer.DaacsOrikaMapper;
import com.daacs.model.SessionedUser;
import com.daacs.model.User;
import com.daacs.model.UserFieldConfig;
import com.daacs.model.UserSearchResult;
import com.daacs.model.dto.CreateUserRequest;
import com.daacs.model.dto.UpdateUserRequest;
import com.daacs.model.event.UserEvent;
import com.daacs.repository.EventContainerRepository;
import com.daacs.repository.UserRepository;
import com.lambdista.util.Try;
import org.opensaml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventContainerRepository eventContainerRepository;

    @Autowired
    private UserFieldConfig userFieldConfig;

    @Autowired
    private DaacsOrikaMapper orikaMapper;

    @Override
    public Try<User> getUser(String id) {
        return userRepository.getUser(id);
    }

    @Override
    public Try<User> getUserByUsername(String username) {
        return userRepository.getUserByUsername(username);
    }

    @Override
    public Try<User> getUserBySecondaryId(String secondaryId) {
        return userRepository.getUserBySecondaryId(secondaryId);
    }

    @Override
    public Try<User> saveUser(User user) {
        Try<Void> maybeResults = userRepository.saveUser(user);
        if(maybeResults.isFailure()){
            return new Try.Failure<>(maybeResults.failed().get());
        }

        return new Try.Success<>(user);
    }

    @Override
    public Try<User> insertUser(User user) {
        Try<Void> maybeResults = userRepository.insertUser(user);
        if(maybeResults.isFailure()){
            return new Try.Failure<>(maybeResults.failed().get());
        }

        return new Try.Success<>(user);
    }

    @Override
    public SessionedUser loadUserBySAML(SAMLCredential credential) throws FailureTypeException {
        String userId = credential.getNameID().getValue();
        List<Attribute> attributes = credential.getAttributes();

        log.info(userId + " is logged in");
        User samlUser = new User(userId, attributes, userFieldConfig);

        //get user from db
        Try<User> maybeUser;
        Boolean loginWithUUID = userFieldConfig.getUseUniqueIdAttributeToLogin();
        if(loginWithUUID != null && loginWithUUID){
            maybeUser = userRepository.getUser(samlUser.getId());
        }
        else{
            maybeUser = userRepository.getUserByUsername(userId);
        }

        if(maybeUser.isFailure()){
            throw (FailureTypeException) maybeUser.failed().get();
        }

        boolean userExists = (maybeUser.get() != null);
        User user = userExists? maybeUser.get() : new User();
        orikaMapper.map(samlUser, user);

        Try<User> maybeSaved = userExists? saveUser(user) : insertUser(user);
        if(maybeSaved.isFailure()){
            throw (FailureTypeException) maybeSaved.failed().get();
        }

        return new SessionedUser(user);
    }

    @Override
    public SessionedUser loadUserByUsername(String username) throws FailureTypeException {
        Try<User> maybeUser = userRepository.getUserByUsername(username);
        if (maybeUser.isFailure()) {
            throw (FailureTypeException) maybeUser.failed().get();
        }
        if (maybeUser.isSuccess() && maybeUser.get() == null) {
            throw new RepoNotFoundException("User");
        }

        return new SessionedUser(maybeUser.get());
    }

    @Override
    public Try<Void> recordEvent(String userId, UserEvent userEvent) {
        return eventContainerRepository.recordUserEvent(userId, userEvent);
    }

    @Override
    public Try<User> updateUser(UpdateUserRequest updateUserRequest){
        Try<User> maybeUser = getUser(updateUserRequest.getId());
        if(maybeUser.isFailure()){
            return new Try.Failure<>(maybeUser.failed().get());
        }

        User user = maybeUser.get();
        orikaMapper.map(updateUserRequest, user);

        return saveUser(user);
    }

    @Override
    public Try<User> createUser(CreateUserRequest createUserRequest){
        User user = new User();
        orikaMapper.map(createUserRequest, user);
        user.setRoles(Collections.singletonList(createUserRequest.getRole()));
        user.setAndEncodePassword(createUserRequest.getPassword());

        return insertUser(user);
    }

    @Override
    public Try<List<UserSearchResult>> searchUsers(List<String> keywords, int limit){
        return userRepository.searchUsers(keywords, limit);
    }

    @Override
    public Try<List<User>> getUsers(List<String> roles){
        return userRepository.getUsers(roles);
    }

    @Override
    public Try<Void> resetPassword(String userId, String password, String code){
        Try<User> maybeUser = getUser(userId);
        if(maybeUser.isFailure()){
            return new Try.Failure<>(maybeUser.failed().get());
        }
        if (maybeUser.isSuccess() && maybeUser.get() == null) {
            return new Try.Failure<>(new RepoNotFoundException("User"));
        }

        User user = maybeUser.get();
        if(user.getResetPasswordCode() == null || !user.getResetPasswordCode().equals(code)){
            return new Try.Failure<>(new RepoNotFoundException("code"));
        }

        user.setResetPasswordCode(null);
        user.setAndEncodePassword(password);

        Try<User> maybeSavedUser = saveUser(user);
        if(maybeSavedUser.isFailure()){
            return new Try.Failure<>(maybeSavedUser.failed().get());
        }

        return new Try.Success<>(null);
    }
}
