package com.daacs.repository.lti;

import com.daacs.framework.exception.NotFoundException;
import com.daacs.model.lti.OutcomeParams;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mgoldman on 6/7/19.
 */
@Repository
@Scope("singleton")
public class OutcomeServiceRepositoryImpl implements OutcomeServiceRepository {

    private Map<String, OutcomeParams> outcomeStore;

    @PostConstruct
    public void init(){
        outcomeStore = Collections.synchronizedMap(new HashMap<>() );
    }

    public String storeOutcomeParams(OutcomeParams params, String userId){
        outcomeStore.put(userId, params);
        return userId;
    }

    public OutcomeParams retrieveOutcomeParams(String userId) throws NotFoundException {
        if(!outcomeStore.containsKey(userId)) {
            throw new NotFoundException("userId " + userId + " does not exist");
        }
        return outcomeStore.remove(userId);
    }
}
