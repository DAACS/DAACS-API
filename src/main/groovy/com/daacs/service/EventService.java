package com.daacs.service;

import com.daacs.model.event.ErrorEvent;
import com.lambdista.util.Try;

/**
 * Created by adistasio on 5/5/17.
 */
public interface EventService {
    Try<Void> recordEvent(ErrorEvent errorEvent);
}
