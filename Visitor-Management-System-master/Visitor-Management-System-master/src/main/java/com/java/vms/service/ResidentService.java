package com.java.vms.service;

import com.java.vms.model.PreApproveDTO;

/***
   *
   * @project Visitor-Management-System
   * @author anvunnam on 12-03-2025.
***/
public interface ResidentService {
    void createPreApprovedVisitReq
            (final PreApproveDTO preApproveDTO,
             final Long userId);
}
