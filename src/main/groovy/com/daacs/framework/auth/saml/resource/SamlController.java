package com.daacs.framework.auth.saml.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.saml.metadata.MetadataManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created by chostetter on 6/29/16.
 */
@RestController
@RequestMapping(value = "/saml", produces = "application/json")
public class SamlController {
    private static final Logger log = LoggerFactory.getLogger(SamlController.class);

    @Autowired
    private MetadataManager metadata;

    private final String baseLoginUrl = "/saml/login?disco=true";

    @RequestMapping(value = "/url", method = RequestMethod.GET, produces = "application/json")
    public Map<String, List<String>> loginUrls(){
        List<String> loginUrls = new ArrayList<>();

        metadata.getIDPEntityNames().stream()
                .map(this::urlEncode)
                .forEach(idp -> loginUrls.add(baseLoginUrl + "&idp=" + idp));

        Map<String, List<String>> response = new HashMap<>();
        response.put("urls", loginUrls);

        System.out.println("\n\n here: "+response);
        return response;
    }

    @SuppressWarnings("deprecation")
    private String urlEncode(String string){
        try{
            return URLEncoder.encode(string, "UTF-8");
        }
        catch(UnsupportedEncodingException ex){
            log.error(ex.getMessage());
            return URLEncoder.encode(string);
        }
    }
}
