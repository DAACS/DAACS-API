package com.daacs.service;

import com.daacs.framework.auth.service.SessionService;
import com.daacs.model.event.ErrorEvent;
import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by adistasio on 5/5/17.
 */
@Service
public class EventServiceImpl implements EventService {
    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);

    @Autowired
    private SessionService sessionService;

    @Override
    public Try<Void> recordEvent(ErrorEvent errorEvent) {

        errorEvent.setUserId(sessionService.getUserId());
        log.error("Error From UI: {}", errorEvent.toString());

        return new Try.Success<>(null);
    }
}
