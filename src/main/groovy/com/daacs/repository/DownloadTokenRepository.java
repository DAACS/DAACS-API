package com.daacs.repository;

import com.daacs.model.User;
import com.lambdista.util.Try;

/**
 * Created by chostetter on 6/28/16.
 */
public interface DownloadTokenRepository {
    String storeUser(User user);
    Try<User> retrieveUser(String token);
}
