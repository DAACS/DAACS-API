package com.daacs.component.queuing;

/**
 * Created by chostetter on 8/9/16.
 */
@FunctionalInterface
public interface Retryable<T> {
    T get() throws Exception;
}
