package com.daacs.framework.auth.repository;

import com.daacs.framework.exception.NotFoundException;
import org.springframework.security.core.Authentication;

/**
 * Created by chostetter on 6/28/16.
 */
public interface AuthenticationRepository {
    String storeAuthenitcation(Authentication authentication);
    Authentication retrieveAuthentication(String token) throws NotFoundException;
}
