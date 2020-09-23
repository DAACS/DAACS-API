package com.daacs.framework.auth.saml;

import com.daacs.framework.auth.saml.filter.SamlOauth2LogoutFilter;
import com.daacs.framework.auth.saml.handler.SamlOauth2LogoutHandler;
import com.daacs.framework.auth.saml.handler.SamlSuccessTokenHandler;
import com.daacs.framework.auth.saml.handler.SimpleLogoutSuccessHandler;
import com.daacs.framework.core.SimpleCORSFilter;
import com.daacs.model.UserFieldConfig;
import com.daacs.service.UserService;
import org.apache.commons.httpclient.HttpClient;
import org.opensaml.saml2.metadata.provider.FilesystemMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.parse.StaticBasicParserPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.saml.*;
import org.springframework.security.saml.key.JKSKeyManager;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.metadata.*;
import org.springframework.security.saml.websso.WebSSOProfileOptions;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by chostetter on 6/27/16.
 * The spring boot security saml example found at https://github.com/vdenotaris/spring-boot-security-saml-sample was
 * used as a starting point for this configuration.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@Order(101)
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SamlConfig extends WebSecurityConfigurerAdapter {

    @Value("${saml.entityId}")
    private String samlEntityId;

    @Value("${saml.frontendAuthSuccessUrl}")
    private String frontendAuthSuccessUrl;

    @Value("${saml.frontendAuthFailureUrl}")
    private String frontendAuthFailureUrl;

    @Value("${saml.idpMetadataPath}")
    private String idpMetadataPath;

    @Value("${saml.keystorePath}")
    private String keystorePath;

    @Value("${saml.keystoreDefaultKey}")
    private String keystoreDefaultKey;

    @Value("${saml.keystorePassword}")
    private String keystorePassword;

    @Value("${saml.userFieldConfig.roleAttribute:@null}")
    private String userFieldConfigRoleAttribute;

    @Value("${saml.userFieldConfig.uniqueIdAttribute:@null}")
    private String userFieldConfigUniqueIdAttribute;

    @Value("${saml.userFieldConfig.firstNameAttribute:@null}")
    private String userFieldConfigFirstNameAttribute;

    @Value("${saml.userFieldConfig.lastNameAttribute:@null}")
    private String userFieldConfigLastNameAttribute;

    @Value("${saml.userFieldConfig.adminRole}")
    private String userFieldConfigAdminRole;

    @Value("${saml.userFieldConfig.studentRole}")
    private String userFieldConfigStudentRole;

    @Value("${saml.userFieldConfig.advisorRole}")
    private String userFieldConfigAdvisorRole;

    @Value("${saml.userFieldConfig.instructorRole}")
    private String userFieldConfigInstructorRole;

    @Value("${saml.useUniqueIdAttributeToLogin:@null}")
    private Boolean useUniqueIdAttributeToLogin;

    @Value("${saml.userFieldConfig.secondaryIdAttribute:@null}")
    private String userFieldConfigSecondaryIdAttribute;

    @Value("${saml.userFieldConfig.canvasSisIdAttribute:@null}")
    private String userFieldConfiCanvasSisIdAttribute;

    @Value("${saml.entityBaseURL:@null}")
    private String entityBaseURL;

    @Autowired
    private UserService userService;

    @Autowired
    private SamlSuccessTokenHandler samlSuccessTokenHandler;

    @Autowired
    private StaticBasicParserPool parserPool;

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private TokenStore oauth2TokenStore;

    @Configuration
    @EnableResourceServer
    @Order(4)
    protected static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

        @Autowired
        private MetadataGeneratorFilter metadataGeneratorFilter;

        @Autowired
        private FilterChainProxy samlFilter;

        @Override
        public void configure(ResourceServerSecurityConfigurer resources) {
            resources.resourceId("restservice");
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.csrf().disable();

            http.addFilterBefore(new SimpleCORSFilter(), ChannelProcessingFilter.class);
            http.addFilterBefore(metadataGeneratorFilter, ChannelProcessingFilter.class);
            http.addFilterAfter(samlFilter, BasicAuthenticationFilter.class);

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
                    .antMatchers(HttpMethod.POST, "/users").permitAll()
                    .anyRequest().authenticated();
        }

    }

    @Bean
    public UserFieldConfig userFieldConfig() {
        return new UserFieldConfig(
                userFieldConfigRoleAttribute,
                userFieldConfigUniqueIdAttribute,
                userFieldConfigFirstNameAttribute,
                userFieldConfigLastNameAttribute,
                userFieldConfigAdminRole,
                userFieldConfigAdvisorRole,
                userFieldConfigInstructorRole,
                userFieldConfigStudentRole,
                useUniqueIdAttributeToLogin,
                userFieldConfigSecondaryIdAttribute,
                userFieldConfiCanvasSisIdAttribute
        );
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService);
        auth.authenticationProvider(samlAuthenticationProvider());
    }

    // SAML Authentication Provider responsible for validating of received SAML
    // messages
    @Bean
    public SAMLAuthenticationProvider samlAuthenticationProvider() {
        SAMLAuthenticationProvider samlAuthenticationProvider = new SAMLAuthenticationProvider();
        samlAuthenticationProvider.setUserDetails(userService);
        samlAuthenticationProvider.setForcePrincipalAsString(false);

        return samlAuthenticationProvider;
    }

    // Key Manager ===================================================================================================//
    // Central storage of cryptographic keys
    @Bean
    public KeyManager keyManager() {

        Resource keystoreFile;
        if (keystorePath.startsWith("classpath")) {
            keystoreFile = (new DefaultResourceLoader()).getResource(keystorePath);
        } else {
            keystoreFile = new FileSystemResource(keystorePath);
        }

        Map<String, String> passwords = new HashMap<>();
        passwords.put(keystoreDefaultKey, keystorePassword);

        return new JKSKeyManager(keystoreFile, keystorePassword, passwords, keystoreDefaultKey);
    }
    // ===============================================================================================================//


    // IdP Config ====================================================================================================//
    @Bean
    public ExtendedMetadataDelegate extendedMetadataProvider() throws MetadataProviderException {

        File metadataFile;
        if (idpMetadataPath.startsWith("classpath")) {
            try {
                metadataFile = (new DefaultResourceLoader()).getResource(idpMetadataPath).getFile();
            } catch (IOException io) {
                throw new RuntimeException("Unable to start application, " + io.getMessage());
            }
        } else {
            metadataFile = new File(idpMetadataPath);
        }

        FilesystemMetadataProvider metadataProvider = new FilesystemMetadataProvider(new Timer(true), metadataFile);
        metadataProvider.setParserPool(parserPool);

        ExtendedMetadataDelegate extendedMetadataDelegate = new ExtendedMetadataDelegate(metadataProvider, extendedMetadata());
        extendedMetadataDelegate.setMetadataTrustCheck(false);
        extendedMetadataDelegate.setMetadataRequireSignature(false);

        return extendedMetadataDelegate;
    }

    // IDP Metadata configuration - paths to metadata of IDPs in circle of trust is here
    // Do no forget to call iniitalize method on providers
    @Bean
    @Qualifier("metadata")
    public CachingMetadataManager metadata() throws MetadataProviderException {
        List<MetadataProvider> providers = new ArrayList<>();
        providers.add(extendedMetadataProvider());

        return new CachingMetadataManager(providers);
    }
    // ===============================================================================================================//


    // Handlers ======================================================================================================//
    // Handler deciding where to redirect user after successful login
    @Bean
    public SamlSuccessTokenHandler authenticationSuccessHandler() {
        samlSuccessTokenHandler.setDefaultTargetUrl(frontendAuthSuccessUrl);
        return samlSuccessTokenHandler;
    }

    // Handler deciding where to redirect user after failed login
    @Bean
    public SimpleUrlAuthenticationFailureHandler authenticationFailureHandler() {
        SimpleUrlAuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();
        failureHandler.setUseForward(false);
        failureHandler.setDefaultFailureUrl(frontendAuthFailureUrl);

        return failureHandler;
    }

    // Handler for successful logout
    @Bean
    public SimpleLogoutSuccessHandler successLogoutHandler() {
        return new SimpleLogoutSuccessHandler();
    }

    // Logout handler terminating local session
    @Bean
    public LogoutHandler logoutHandler() {
        return new SamlOauth2LogoutHandler(oauth2TokenStore, userService);
    }
    // ===============================================================================================================//


    // Endpoints =====================================================================================================//
    @Bean
    public WebSSOProfileOptions defaultWebSSOProfileOptions() {
        WebSSOProfileOptions webSSOProfileOptions = new WebSSOProfileOptions();
        webSSOProfileOptions.setIncludeScoping(false);

        return webSSOProfileOptions;
    }

    // Entry point to initialize authentication, default values taken from
    // properties file
    @Bean
    public SAMLEntryPoint samlEntryPoint() {
        WebSSOProfileOptions webSSOProfileOptions = new WebSSOProfileOptions();
        webSSOProfileOptions.setIncludeScoping(false);

        SAMLEntryPoint samlEntryPoint = new SAMLEntryPoint();
        samlEntryPoint.setDefaultProfileOptions(defaultWebSSOProfileOptions());

        return samlEntryPoint;
    }

    // Setup advanced info about metadata
    @Bean
    public ExtendedMetadata extendedMetadata() {
        ExtendedMetadata extendedMetadata = new ExtendedMetadata();
        extendedMetadata.setIdpDiscoveryEnabled(true);
        extendedMetadata.setSignMetadata(false);

        return extendedMetadata;
    }

    // IDP Discovery Service
    @Bean
    public SAMLDiscovery samlIDPDiscovery() {
        SAMLDiscovery idpDiscovery = new SAMLDiscovery();
        idpDiscovery.setIdpSelectionPath("/saml/idpSelection");

        return idpDiscovery;
    }
    // ===============================================================================================================//


    // Filters =======================================================================================================//
    // Filter processing incoming logout messages
    // First argument determines URL user will be redirected to after successful
    // global logout
    @Bean
    public SAMLLogoutProcessingFilter samlLogoutProcessingFilter() {
        return new SAMLLogoutProcessingFilter(successLogoutHandler(), logoutHandler());
    }

    // Overrides default logout processing filter with the one processing SAML
    // messages
    @Bean
    public SAMLLogoutFilter samlLogoutFilter() {
        return new SamlOauth2LogoutFilter(oauth2TokenStore, successLogoutHandler(),
                new LogoutHandler[]{logoutHandler()},
                new LogoutHandler[]{logoutHandler()});
    }

    @Bean
    public SAMLWebSSOHoKProcessingFilter samlWebSSOHoKProcessingFilter() throws Exception {
        SAMLWebSSOHoKProcessingFilter samlWebSSOHoKProcessingFilter = new SAMLWebSSOHoKProcessingFilter();
        samlWebSSOHoKProcessingFilter.setAuthenticationSuccessHandler(authenticationSuccessHandler());
        samlWebSSOHoKProcessingFilter.setAuthenticationManager(authenticationManager());
        samlWebSSOHoKProcessingFilter.setAuthenticationFailureHandler(authenticationFailureHandler());

        return samlWebSSOHoKProcessingFilter;
    }

    // Processing filter for WebSSO profile messages
    @Bean
    public SAMLProcessingFilter samlWebSSOProcessingFilter() throws Exception {
        SAMLProcessingFilter samlWebSSOProcessingFilter = new SAMLProcessingFilter();
        samlWebSSOProcessingFilter.setAuthenticationManager(authenticationManager());
        samlWebSSOProcessingFilter.setAuthenticationSuccessHandler(authenticationSuccessHandler());
        samlWebSSOProcessingFilter.setAuthenticationFailureHandler(authenticationFailureHandler());

        return samlWebSSOProcessingFilter;
    }

    // Filter automatically generates default SP metadata
    @Bean
    public MetadataGenerator metadataGenerator() {
        MetadataGenerator metadataGenerator = new MetadataGenerator();
        metadataGenerator.setEntityId(samlEntityId);
        metadataGenerator.setExtendedMetadata(extendedMetadata());
        metadataGenerator.setIncludeDiscoveryExtension(false);
        metadataGenerator.setKeyManager(keyManager());

        if (entityBaseURL != null) {
            metadataGenerator.setEntityBaseURL(entityBaseURL);
        }

        return metadataGenerator;
    }

    // The filter is waiting for connections on URL suffixed with filterSuffix
    // and presents SP metadata there
    @Bean
    public MetadataDisplayFilter metadataDisplayFilter() {
        return new MetadataDisplayFilter();
    }

    @Bean
    public MetadataGeneratorFilter metadataGeneratorFilter() {
        return new MetadataGeneratorFilter(metadataGenerator());
    }

    @Bean
    public FilterChainProxy samlFilter() throws Exception {
        List<SecurityFilterChain> chains = new ArrayList<>();
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/login/**"), samlEntryPoint()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/logout/**"), samlLogoutFilter()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/metadata/**"), metadataDisplayFilter()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SSO/**"), samlWebSSOProcessingFilter()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SSOHoK/**"), samlWebSSOHoKProcessingFilter()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SingleLogout/**"), samlLogoutProcessingFilter()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/discovery/**"), samlIDPDiscovery()));

        return new FilterChainProxy(chains);
    }
    // ===============================================================================================================//

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
