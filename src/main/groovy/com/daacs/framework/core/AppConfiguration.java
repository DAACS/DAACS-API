package com.daacs.framework.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alandistasio on 8/5/16.
 */
@Configuration
public class AppConfiguration {
    private static final Logger log = LoggerFactory.getLogger(AppConfiguration.class);

    @Bean
    public SmartValidator validator(){
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public PropertyPlaceholderConfigurer properties() {
        final PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
        ppc.setIgnoreResourceNotFound(true);
        ppc.setNullValue("@null");

        final List<Resource> resourceLst = new ArrayList<>();

        resourceLst.add(new ClassPathResource("application.properties"));

        if(System.getenv("daacsapiProperties") == null){
            log.error("Please set the daacsapiProperties environment variable. Using default application.properties.");
        }
        else{
            resourceLst.add(new FileSystemResource(System.getenv("daacsapiProperties")));
        }

        ppc.setLocations(resourceLst.toArray(new Resource[]{}));

        return ppc;
    }

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter crlf = new CommonsRequestLoggingFilter();
        crlf.setIncludeClientInfo(true);
        crlf.setIncludeQueryString(true);
        crlf.setIncludePayload(true);
        crlf.setMaxPayloadLength(4000);
        return crlf;
    }
}
