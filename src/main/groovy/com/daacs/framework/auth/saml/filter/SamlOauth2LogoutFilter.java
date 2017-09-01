package com.daacs.framework.auth.saml.filter;

import org.opensaml.common.SAMLException;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.saml.SAMLConstants;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.SAMLLogoutFilter;
import org.springframework.security.saml.context.SAMLMessageContext;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.util.Assert;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by chostetter on 6/29/16.
 */
public class SamlOauth2LogoutFilter extends SAMLLogoutFilter {

    private static final String BEARER_AUTHENTICATION = "Bearer ";
    private static final String HEADER_AUTHORIZATION = "authorization";

    private TokenStore tokenStore;

    public SamlOauth2LogoutFilter(TokenStore tokenStore, LogoutSuccessHandler logoutSuccessHandler, LogoutHandler[] localHandler, LogoutHandler[] globalHandlers) {
        super(logoutSuccessHandler, localHandler, globalHandlers);
        this.tokenStore = tokenStore;
    }

    @Override
    public void processLogout(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (requiresLogout(request, response)) {

            String token = request.getHeader(HEADER_AUTHORIZATION);

            if (token == null || !token.startsWith(BEARER_AUTHENTICATION) || token.split(" ").length != 2) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            OAuth2Authentication oAuth2Authentication = tokenStore.readAuthentication(token.split(" ")[1]);

            if (oAuth2Authentication == null) {
                chain.doFilter(request, response);
                return;
            }

            Authentication auth = oAuth2Authentication.getUserAuthentication();

            try {

                if (auth != null && isGlobalLogout(request, auth)) {

                    Assert.isInstanceOf(SAMLCredential.class, auth.getCredentials(), "Authentication object doesn't contain SAML credential, cannot perform global logout");

                    // Notify session participants using SAML Single Logout profile
                    SAMLCredential credential = (SAMLCredential) auth.getCredentials();
                    request.setAttribute(SAMLConstants.LOCAL_ENTITY_ID, credential.getLocalEntityID());
                    request.setAttribute(SAMLConstants.PEER_ENTITY_ID, credential.getRemoteEntityID());
                    SAMLMessageContext context = contextProvider.getLocalAndPeerEntity(request, response);
                    profile.sendLogoutRequest(context, credential);
                    samlLogger.log(SAMLConstants.LOGOUT_REQUEST, SAMLConstants.SUCCESS, context);
                }
                else {
                    chain.doFilter(request, response);
                }

            } catch (SAMLException e) {
                logger.debug("Error initializing global logout", e);
                throw new ServletException("Error initializing global logout", e);
            } catch (MetadataProviderException e) {
                logger.debug("Error processing metadata", e);
                throw new ServletException("Error processing metadata", e);
            } catch (MessageEncodingException e) {
                logger.debug("Error encoding outgoing message", e);
                throw new ServletException("Error encoding outgoing message", e);
            }
        }
        else{
            chain.doFilter(request, response);
        }
    }
}
