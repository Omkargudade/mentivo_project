package com.mentivo.repository;

import com.mentivo.entity.User;
import com.mentivo.entity.enums.CurrentMode;
import com.mentivo.entity.enums.PrimaryRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByPrimaryRole(PrimaryRole primaryRole, Pageable pageable);

    Page<User> findByCurrentMode(CurrentMode currentMode, Pageable pageable);

    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.headline) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);

    long countByPrimaryRole(PrimaryRole primaryRole);
    long countByIsActive(boolean isActive);
}
