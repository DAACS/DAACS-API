package com.daacs.repository.lti;

import com.daacs.model.Nonce;
import com.lambdista.util.Try;

/**
 * Created by mgoldman on 2/27/19.
 */
public interface LtiNonceRepository {

    Try<Nonce> getNonce(String id);
    Try<Void> insertNonce(Nonce nonce);
}
