package com.unisal.selectaapi.auth.oauth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.unisal.selectaapi.auth.service.AuthService;
import com.unisal.selectaapi.auth.service.OAuthProviderService;
import com.unisal.selectaapi.exception.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthSuccessHandler.class);
    private final Map<String, OAuthProviderService> providerServices;
    private final AuthService authService;
    private final String frontendUrl;

    public OAuthSuccessHandler(
            Map<String, OAuthProviderService> providerServices,
            AuthService authService,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.providerServices = providerServices;
        this.authService = authService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        OAuthProviderService providerService = providerServices.get(registrationId);

        if (providerService == null || !(oauthToken.getPrincipal() instanceof OidcUser oidcUser)) {
                redirectFailure(request, response, "oauth_failed", registrationId);
                return;
        }

        try {

            var user = providerService.authenticateCandidate(oidcUser);
            var token = authService.issueToken(user);

            String fragment = "token=" + URLEncoder.encode(token.tokenJwt(), StandardCharsets.UTF_8)
                    + "&refresh=" + URLEncoder.encode(token.refreshToken(), StandardCharsets.UTF_8)
                    + "&expires=" + token.expiresInJwt();

            invalidateSession(request);
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            response.sendRedirect(frontendUrl + "/auth/callback#" + fragment);
        } catch (BusinessException exception) {
            LOGGER.warn("{} autenticou, mas a conta foi recusada: status={}, reason={}", registrationId,
                    exception.getStatus().value(), exception.getMessage());
            String errorCode = exception.getStatus().value() == 403
                    ? "account_type"
                    : "email_unavailable";
            redirectFailure(request, response, errorCode, registrationId);
        }
    }

    private void redirectFailure(HttpServletRequest request, HttpServletResponse response, String errorCode, String registrationId)
            throws IOException {
        invalidateSession(request);
        response.sendRedirect(frontendUrl + "/auth/candidato?oauthError=" + errorCode + "&provider=" +  registrationId);
    }

    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}