package com.daacs.framework.auth.lti;

import com.daacs.framework.core.SimpleCORSFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class LtiAuthConfig extends WebSecurityConfigurerAdapter {

    @Configuration
    @EnableResourceServer
    @Order(3)
    protected static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

        @Override
        public void configure(ResourceServerSecurityConfigurer resources) {
            resources.resourceId("restservice");
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.csrf().disable();

            http.addFilterBefore(new SimpleCORSFilter(), ChannelProcessingFilter.class);

            http.sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .authorizeRequests()
                    .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .antMatchers("/oauth/**").permitAll()
                    .antMatchers("/saml/**").permitAll()
                    .antMatchers("/swagger*/**").permitAll()
                    .antMatchers("/webjars/springfox-swagger-ui/**").permitAll()
                    .antMatchers("/v2/api-docs").permitAll()
                    .antMatchers("/download/token").authenticated()
                    .antMatchers("/download/**").permitAll()
                    .antMatchers("/forgot-password").permitAll()
                    .antMatchers("/class-scores/download").permitAll()
                    .antMatchers("/error-events").permitAll()
                    .antMatchers("/lti").permitAll()
                    .antMatchers(HttpMethod.POST, "/users").permitAll()
                    .anyRequest().authenticated();
        }

    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
    }

}
