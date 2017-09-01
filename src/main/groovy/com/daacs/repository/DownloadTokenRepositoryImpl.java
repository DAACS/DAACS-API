package com.daacs.repository;

import com.daacs.framework.exception.RepoNotFoundException;
import com.daacs.model.User;
import com.lambdista.util.Try;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Created by chostetter on 6/28/16.
 */
@Repository
@Scope("singleton")
public class DownloadTokenRepositoryImpl implements DownloadTokenRepository {

    private int TOKEN_TTL = 10000;
    private Map<String, User> tokenStore;

    @PostConstruct
    public void init(){
        tokenStore = Collections.synchronizedMap(new PassiveExpiringMap<>(TOKEN_TTL));
    }

    @Override
    public String storeUser(User user){
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, user);

        return token;
    }

    @Override
    public Try<User> retrieveUser(String token) {
        if(!tokenStore.containsKey(token)) {
            return new Try.Failure<>(new RepoNotFoundException("token"));
        }

        return new Try.Success<>(tokenStore.remove(token));
    }
}
