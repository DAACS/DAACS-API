package com.daacs.model.dto

import com.daacs.framework.validation.annotations.ValidCreateUserRequest

import javax.validation.constraints.NotNull

/**
 * Created by adistasio on 1/2/17.
 */
@ValidCreateUserRequest
class CreateUserRequest {

    @NotNull
    String username;

    @NotNull
    String password;

    @NotNull
    String passwordConfirm;

    @NotNull
    String firstName;

    @NotNull
    String lastName;

    @NotNull
    String role;
}

