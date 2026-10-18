package com.unisal.selectaapi.auth.service;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtPrincipal {

    private JwtPrincipal() {
    }

    public static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    public static UUID sessionId(Jwt jwt) {
        String sid = jwt.getClaimAsString("sid");
        if (sid == null) {
            return null;
        }
        try {
            return UUID.fromString(sid);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
