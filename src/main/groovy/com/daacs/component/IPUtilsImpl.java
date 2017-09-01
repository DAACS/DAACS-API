package com.daacs.component;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by adistasio on 5/5/17.
 */
@Component
public class IPUtilsImpl implements IPUtils {

    @Override
    public String getIPAddress(HttpServletRequest request) {
        String realIP = request.getHeader("X-Real-IP");

        return realIP != null ? realIP : request.getRemoteAddr();
    }
}
