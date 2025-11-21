package com.java.vms.service;

import com.java.vms.domain.User;
import com.java.vms.model.UserDTO;
import com.java.vms.util.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

/***
   *
   * @project Visitor-Management-System
   * @author anvunnam on 12-03-2025.
   ***/
public interface UserService {

    List<UserDTO> findAll();

    UserDTO get(Long id);

    public Long create
            (final @Valid UserDTO userDTO)
            throws SQLIntegrityConstraintViolationException;

    void update(final UserDTO userDTO);

    void markUserStatus(Long id) throws NotFoundException;

    List<String> createUsersFromFile(MultipartFile file);

    User loadUserByUsername(String username) throws UsernameNotFoundException;
}
