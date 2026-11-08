package com.unisal.selectaapi.user;

import com.unisal.selectaapi.exception.BusinessException;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import jakarta.validation.Valid;

import com.unisal.selectaapi.user.dto.CreateUserDto;
import com.unisal.selectaapi.user.dto.UpdateUserDto;
import com.unisal.selectaapi.user.dto.UserResponseDto;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody CreateUserDto dto) {
        var response = userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> findAllUsers() {
        var response = userService.findAllUsers();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> findCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.findCurrentUser(UUID.fromString(jwt.getSubject())));
    }

    @GetMapping("/{id:[0-9a-fA-F-]{36}}")
    public ResponseEntity<UserResponseDto> findUserById(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        requireOwnerOrAdmin(jwt, id);
        var response = userService.findUserById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id:[0-9a-fA-F-]{36}}")
    public ResponseEntity<UserResponseDto> updateUser(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id, @Valid @RequestBody UpdateUserDto dto) {
        requireOwnerOrAdmin(jwt, id);
        if (!isAdmin(jwt) && (dto.role() != null || dto.active() != null)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Apenas administradores podem alterar role ou active");
        }
        var response = userService.updateUser(id, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id:[0-9a-fA-F-]{36}}")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        requireOwnerOrAdmin(jwt, id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    private void requireOwnerOrAdmin(Jwt jwt, UUID id) {
        if (!id.equals(UUID.fromString(jwt.getSubject())) && !isAdmin(jwt)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Acesso restrito ao titular da conta");
        }
    }

    private boolean isAdmin(Jwt jwt) {
        return "ADMIN".equals(jwt.getClaimAsString("scope"));
    }
}
