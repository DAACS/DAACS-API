package com.daacs.framework.auth.saml.handler;

import com.daacs.framework.auth.saml.repository.AuthenticationRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by chostetter on 6/27/16.
 */
@Component
public class SamlSuccessTokenHandler extends SimpleUrlAuthenticationSuccessHandler {

    protected final Log log = LogFactory.getLog(SamlSuccessTokenHandler.class);

    @Autowired
    private AuthenticationRepository samlAuthenticationRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {

        clearAuthenticationAttributes(request);

        String token = samlAuthenticationRepository.storeAuthenitcation(authentication);

        // Use the DefaultSavedRequest URL

        String targetUrl = getTokenTargetUrl(token);
        logger.debug("Redirecting to token Url: " + targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    @SuppressWarnings("deprecation")
    private String getTokenTargetUrl(String token){
        String targetUrl = getDefaultTargetUrl();
        String appendCharacter = "?";
        if(targetUrl.contains("?")){
            appendCharacter = "&";
        }

        targetUrl += appendCharacter + "token=";

        try{
            targetUrl += URLEncoder.encode(token, "UTF-8");
        }
        catch(UnsupportedEncodingException uee){
            log.error(uee);
            targetUrl += URLEncoder.encode(token);
        }

        return targetUrl;
    }
}

