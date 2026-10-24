package com.unisal.selectaapi.company.dto;

import java.util.List;

public record CompanyMembersDto(boolean podeGerencias, List<CompanyMemberDto> companyMembersDto) {
}
