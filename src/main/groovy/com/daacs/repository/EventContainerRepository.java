package com.daacs.repository;

import com.daacs.model.event.UserEvent;
import com.lambdista.util.Try;

/**
 * Created by chostetter on 12/14/15.
 */
public interface EventContainerRepository {
    Try<Void> recordUserEvent(String userId, UserEvent userEvent);
}
