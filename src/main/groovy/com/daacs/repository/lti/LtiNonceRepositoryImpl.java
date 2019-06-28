package com.daacs.repository.lti;

import com.daacs.component.HystrixCommandFactory;
import com.daacs.framework.exception.AlreadyExistsException;
import com.daacs.model.Nonce;
import com.daacs.repository.lti.LtiNonceRepository;
import com.lambdista.util.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * Created by mgoldman on 2/27/19.
 */
@Repository
public class LtiNonceRepositoryImpl implements LtiNonceRepository {

    @Autowired
    private HystrixCommandFactory hystrixCommandFactory;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Try<Nonce> getNonce(String id) {
        return hystrixCommandFactory.getMongoFindByIdCommand("LtiNonceRepository-getNonce", mongoTemplate, id, Nonce.class).execute();
    }


    @Override
    public synchronized Try<Void> insertNonce(Nonce nonce) {

        Try<Nonce> maybeNonce = getNonce(nonce.getId());

        //check if nonce already exists
        if (maybeNonce.isFailure()) {
            return new Try.Failure<>(maybeNonce.failed().get());
        }

        if (maybeNonce.toOptional().isPresent()) {
            return new Try.Failure<>(new AlreadyExistsException("Nonce", "id", nonce.getId()));
        }
        //

        return hystrixCommandFactory.getMongoInsertCommand("LtiNonceRepository-insertNonce", mongoTemplate, nonce).execute();
    }

}
