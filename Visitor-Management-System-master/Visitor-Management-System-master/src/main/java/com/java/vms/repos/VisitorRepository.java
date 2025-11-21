package com.java.vms.repos;

import com.java.vms.domain.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface VisitorRepository extends JpaRepository<Visitor, Long> {

    Optional<Visitor> findVisitorByUnqId(String unqId);

    boolean existsByPhoneIgnoreCase(String phone);

    boolean existsByUnqIdIgnoreCase(String unqId);

}
