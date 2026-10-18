package com.unisal.selectaapi.company.repository;

import com.unisal.selectaapi.company.entity.CompanyMember;
import com.unisal.selectaapi.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, UUID> {

    @Query("""
        SELECT m FROM CompanyMember m
            WHERE m.company.id = (
                SELECT cm.company.id
                FROM CompanyMember cm
                WHERE cm.user.id = :user_id
            )
    """)
    List<CompanyMember> pegarMembrosDaMesmaEmpresa(@Param("user_id") UUID userId);

    Optional<CompanyMember> findByUserId(UUID userId);

    //Pega funcionarios
    List<CompanyMember> findAllByCompany_User_Id(UUID companyUserId);

    List<User> findAllUsersByCompanyId(UUID companyId);

    boolean existsByUserId(UUID userId);

    UUID userId(UUID userId);
}
