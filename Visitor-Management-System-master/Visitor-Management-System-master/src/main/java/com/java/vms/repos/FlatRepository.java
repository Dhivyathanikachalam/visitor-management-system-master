package com.java.vms.repos;

import com.java.vms.domain.Flat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface FlatRepository extends JpaRepository<Flat, Long> {

    boolean existsByFlatNumIgnoreCase(String flatNum);

    Optional<Flat> findByFlatNum(String flatNum);

}
