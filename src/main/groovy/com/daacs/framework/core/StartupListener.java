package com.daacs.framework.core;

import com.daacs.component.queuing.QueueListener;
import com.daacs.service.LightSideService;
import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Created by chostetter on 8/10/16.
 */
@Component
public class StartupListener implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger log = LoggerFactory.getLogger(StartupListener.class);

    @Autowired
    private QueueListener queueListener;

    @Autowired
    private LightSideService lightSideService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        Try<Void> maybeLightSideSetup = lightSideService.setupFileSystem();
        if(maybeLightSideSetup.isFailure()){
            throw new RuntimeException(maybeLightSideSetup.failed().get());
        }

        queueListener.startListening();
    }
}