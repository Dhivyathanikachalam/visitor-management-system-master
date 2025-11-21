package com.java.vms.repos;

import com.java.vms.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(Long phone);

    Optional<User> findUserByEmail(String email);

    Optional<User> findUserByNameAndPhone(String name, Long phoneNumber);
}
