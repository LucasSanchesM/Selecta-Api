package com.unisal.selectaapi.user;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/me/preferences")
public class UserPreferencesController {
    private final UserPreferencesService service;

    public UserPreferencesController(UserPreferencesService service) {
        this.service = service;
    }

    @GetMapping
    public UserPreferences get(@AuthenticationPrincipal Jwt jwt) {
        return service.get(UUID.fromString(jwt.getSubject()));
    }

    @PutMapping
    public UserPreferences update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UserPreferences preferences) {
        return service.update(UUID.fromString(jwt.getSubject()), preferences);
    }
}
