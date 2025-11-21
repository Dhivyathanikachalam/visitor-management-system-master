package com.java.vms.service;

import com.java.vms.domain.Flat;
import com.java.vms.model.FlatDTO;
import com.java.vms.model.FlatStatus;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

/***
 *
 * @project Visitor-Management-System
 * @author anvunnam on 10-03-2025.
 ***/
public interface FlatService {

    List<FlatDTO> findAll();

    FlatDTO get(Long id);

    Long create(FlatDTO flatDTO) throws SQLIntegrityConstraintViolationException;

    Flat update(FlatDTO flatDTO);

    FlatStatus changeFlatStatusToNotAvailable(String flatNum, boolean status);
}
