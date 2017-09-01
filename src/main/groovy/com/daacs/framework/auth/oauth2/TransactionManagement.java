package com.daacs.framework.auth.oauth2;

import org.springframework.context.annotation.Configuration;
import org.springframework.integration.transaction.PseudoTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;

/**
 * Created by chostetter on 7/5/16.
 *
 * Spring doesn't like not having a transaction manager, even though we don't need one. For anything.
 * https://github.com/spring-projects/spring-security-oauth/issues/310
 */
@Configuration
@EnableTransactionManagement
public class TransactionManagement implements TransactionManagementConfigurer {
    @Override
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        return new PseudoTransactionManager();
    }
}