package com.sodasplash.auth_service.repository;

import com.sodasplash.auth_service.entity.Role;
import com.sodasplash.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByRoleIn(List<Role> roles);
}
