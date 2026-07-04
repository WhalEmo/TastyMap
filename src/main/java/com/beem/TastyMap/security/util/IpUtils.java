package com.beem.TastyMap.security.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class IpUtils {
    public static String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return "0.0.0.0";
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getRemoteAddr();
        return (ip == null || ip.isEmpty()) ? "0.0.0.0" : ip;
    }
}
