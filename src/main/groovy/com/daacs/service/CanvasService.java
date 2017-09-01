package com.daacs.service;

import com.lambdista.util.Try;

/**
 * Created by chostetter on 4/6/17.
 */
public interface CanvasService {
    boolean isEnabled();
    Try<String> markAssignmentCompleted(String sisId);
}
