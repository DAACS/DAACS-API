package com.daacs.service;


import com.daacs.model.User;
import com.daacs.model.UserSearchResult;
import com.daacs.model.dto.CreateUserRequest;
import com.daacs.model.dto.UpdateUserRequest;
import com.daacs.model.event.UserEvent;
import com.lambdista.util.Try;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;

import java.util.List;

public interface UserService extends UserDetailsService, SAMLUserDetailsService {
    Try<User> getUser(String id);
    Try<User> getUserIfExists(String username);
    Try<User> saveUser(User user);
    Try<User> insertUser(User user);
    Try<Void> recordEvent(String userId, UserEvent userEvent);
    Try<User> updateUser(UpdateUserRequest updateUserRequest);
    Try<User> createUser(CreateUserRequest createUserRequest);
    Try<List<UserSearchResult>> searchUsers(List<String> keywords,String roleString, int limit);
    Try<User> getUserByUsername(String username);
    Try<User> getUserBySecondaryId(String secondaryId);
    Try<Void> resetPassword(String userId, String password, String code);
    Try<List<User>> getUsers(List<String> roles);
    Try<User> getInstructorById(String id);
    Try<List<User>> getUsersById(List<String> ids);
}