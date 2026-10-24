package com.unisal.selectaapi.company.dto;

import com.unisal.selectaapi.company.entity.CompanyMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCompanyMemberDto(@NotBlank(message = "O campo 'name' é obrigatório")
                                     String name,

                                     @NotBlank(message = "O campo 'email' é obrigatório")
                                     @Email(message = "O campo 'email' deve ser um e-mail válido")
                                     String email,

                                     @NotBlank(message = "O campo 'password' é obrigatório")
                                     String password,

                                     @NotNull(message = "O campo 'role' é obrigatorio")
                                     CompanyMemberRole role
                                    ) {}
