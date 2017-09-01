package com.daacs.framework.auth.oauth2;

import com.daacs.framework.auth.saml.repository.AuthenticationRepository;
import com.daacs.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.config.annotation.builders.InMemoryClientDetailsServiceBuilder;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.CompositeTokenGranter;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.refresh.RefreshTokenGranter;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alandistasio on 5/17/15.
 */
@Configuration
public class OAuth2Config {

    @Configuration
    @EnableAuthorizationServer
    @Order(3)
    protected static class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {

        @Autowired
        @Qualifier("authenticationManagerBean")
        private AuthenticationManager authenticationManager;

        @Autowired
        private UserService userDetailsService;

        @Autowired
        private AuthenticationRepository samlAuthenticationRepository;

        @Override
        public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
            oauthServer.allowFormAuthenticationForClients();
        }

        //this is a workaround because of a bug in spring oauth2. There is a fix but it is in 2.0.9:
        //https://github.com/spring-projects/spring-security-oauth/commit/0c0888ffbd321afb5ac910a33169b01afe9e92c2
        @Override
        public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
            endpoints
                    .tokenStore(tokenStore())
                    .authenticationManager(authenticationManager)
                    .userDetailsService(userDetailsService)
                    .tokenGranter(tokenGranter(clientDetailsService(), oAuth2RequestFactory(), tokenServices()))
                    .requestFactory(oAuth2RequestFactory())
                    .tokenServices(tokenServices())
                    .setClientDetailsService(clientDetailsService())
                    ;
        }

        @Bean
        public ClientDetailsService clientDetailsService() throws Exception {
            return new InMemoryClientDetailsServiceBuilder()
                    .withClient("web")
                    .and().build();
        }

        @Bean
        public OAuth2RequestFactory oAuth2RequestFactory() throws Exception {
            return new DefaultOAuth2RequestFactory(clientDetailsService());
        }

        private TokenGranter tokenGranter(ClientDetailsService clientDetailsService,
                                          OAuth2RequestFactory oAuth2RequestFactory,
                                          AuthorizationServerTokenServices tokenService) {

            List<TokenGranter> granters = new ArrayList<>();

            granters.add(new RefreshTokenGranter(
                    tokenService,
                    clientDetailsService,
                    oAuth2RequestFactory));

            granters.add(new UserTokenGranter(
                    tokenService,
                    clientDetailsService,
                    oAuth2RequestFactory,
                    userDetailsService,
                    tokenStore()));

            granters.add(new SamlTokenGranter(
                    samlAuthenticationRepository,
                    tokenService,
                    clientDetailsService,
                    oAuth2RequestFactory));

            return new CompositeTokenGranter(granters);
        }

        @Bean
        public TokenStore tokenStore() {
            return new InMemoryTokenStore();
        }

        @Primary
        @Bean
        public AuthorizationServerTokenServices tokenServices() {
            final DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
            defaultTokenServices.setSupportRefreshToken(true);
            defaultTokenServices.setTokenStore(tokenStore());

            return defaultTokenServices;
        }
    }

}
