package com.daacs.repository;

import com.daacs.model.User;
import com.daacs.model.UserSearchResult;
import com.lambdista.util.Try;

import java.util.List;

/**
 * Created by alandistasio on 9/12/15.
 */
public interface UserRepository {

    Try<User> getUser(String id);
    Try<User> getUserIfExists(String username);
    Try<User> getUserByUsername(String username);
    Try<User> getUserBySecondaryId(String secondaryId);
    Try<Void> saveUser(User user);
    Try<Void> insertUser(User user);
    Try<List<UserSearchResult>> searchUsers(List<String> keywords, String roleString, int limit);
    Try<List<User>> getUsers(List<String> roles);
    Try<User> getUserByRoleAndId(String id, List<String> roles);
    Try<List<User>> getUsersById(List<String> ids);
}
