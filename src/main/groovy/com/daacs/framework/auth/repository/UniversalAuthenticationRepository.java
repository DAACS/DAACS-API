package com.daacs.framework.auth.repository;

import com.daacs.framework.exception.NotFoundException;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Created by chostetter on 6/28/16.
 *
 * used by saml and lti login
 */
@Repository
@Scope("singleton")
public class UniversalAuthenticationRepository implements AuthenticationRepository {

    private int TOKEN_TTL = 30000;
    private Map<String, Authentication> authStore;

    @PostConstruct
    public void init(){
        authStore = Collections.synchronizedMap(new PassiveExpiringMap<>(TOKEN_TTL));
    }

    public String storeAuthenitcation(Authentication authentication){
        String token = UUID.randomUUID().toString();
        authStore.put(token, authentication);

        return token;
    }

    public Authentication retrieveAuthentication(String token) throws NotFoundException {
        if(!authStore.containsKey(token)) {
            throw new NotFoundException("Token " + token + " does not exist");
        }

        return authStore.remove(token);
    }
}
