package com.unisal.selectaapi.user;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.unisal.selectaapi.user.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockById(UUID id);

    Optional<User> findByEmail(String email);

    Optional<User> findByLinkedinSubject(String linkedinSubject);

    Optional<User> findByGoogleSubject(String googleSubject);

    boolean existsByEmail(String email);
}
