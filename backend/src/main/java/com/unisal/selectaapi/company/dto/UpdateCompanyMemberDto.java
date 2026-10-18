package com.unisal.selectaapi.company.dto;

import com.unisal.selectaapi.company.entity.CompanyMemberRole;

import java.util.UUID;

public record UpdateCompanyMemberDto(String name,
                                     String password,
                                     CompanyMemberRole role) {
}
