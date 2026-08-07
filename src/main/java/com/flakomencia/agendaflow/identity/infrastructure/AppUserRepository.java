package com.flakomencia.agendaflow.identity.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.flakomencia.agendaflow.identity.domain.AppUser;

import jakarta.persistence.LockModeType;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select user from AppUser user
            where lower(user.email) = :normalizedEmail
              and user.deletedAt is null
            """)
    Optional<AppUser> findForAuthentication(@Param("normalizedEmail") String normalizedEmail);

    Optional<AppUser> findByIdAndDeletedAtIsNull(Long id);
}
