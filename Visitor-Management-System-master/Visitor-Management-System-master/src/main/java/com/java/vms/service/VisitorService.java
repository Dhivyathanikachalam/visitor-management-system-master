package com.java.vms.service;

import com.java.vms.model.PreApproveDTO;
import com.java.vms.model.VisitorDTO;

import java.util.List;

/***
   *
   * @project Visitor-Management-System
   * @author anvunnam on 12-03-2025.
   ***/

public interface VisitorService {
    List<VisitorDTO> findAll();

    VisitorDTO get(Long id);

    Long create(VisitorDTO visitorDTO);

    Long create(PreApproveDTO preApproveDTO);

    void update(Long id, VisitorDTO visitorDTO);

    boolean unqIdExists(String unqId);
}
