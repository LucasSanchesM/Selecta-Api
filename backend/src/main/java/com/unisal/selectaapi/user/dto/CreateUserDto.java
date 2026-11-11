package com.unisal.selectaapi.user.dto;

import com.unisal.selectaapi.user.UserRole;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserDto(
    @NotBlank(message = "O campo 'name' é obrigatório")
    String name,

    @NotBlank(message = "O campo 'email' é obrigatório")
    @Email(message = "O campo 'email' deve ser um e-mail válido")
    String email,

    @NotBlank(message = "O campo 'password' é obrigatório")
    String password,

    UserRole role,
    @Valid InitialProfile profile
) {
    public record InitialProfile(@Size(max = 200) String headline, @Size(max = 150) String city,
                                 @Size(max = 2) String state) {}

    public CreateUserDto(String name, String email, String password, UserRole role) {
        this(name, email, password, role, null);
    }
}
