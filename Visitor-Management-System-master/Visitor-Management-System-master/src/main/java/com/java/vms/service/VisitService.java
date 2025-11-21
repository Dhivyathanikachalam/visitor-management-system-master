package com.java.vms.service;

import com.java.vms.model.VisitDTO;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/***
   *
   * @project Visitor-Management-System
   * @author anvunnam on 12-03-2025.
   ***/

public interface VisitService {
    List<VisitDTO> findAll(Pageable pageable);

    VisitDTO get(final Long id);

    Long create(final VisitDTO visitDTO) throws BadRequestException;

    void update(final Long id, final VisitDTO visitDTO) throws BadRequestException;

    Long anyPreApprovedExists(Long visitorId, Long userId) throws BadRequestException;

    void markVisitorEntry(Long visitId) throws BadRequestException;

    void markVisitorExit(Long visitId) throws BadRequestException;

    void approveVisitReq(Long visitId) throws BadRequestException;

    void rejectVisitReq(Long visitId) throws BadRequestException;

    List<VisitDTO> listAllVisitReqsByStatus
            (String status,
             String userName,
             Long userPhone,
             boolean isDurationEnabled)
            throws BadRequestException;

    String uploadVisitorImage(MultipartFile file);

    byte[] getAllVisitRequestsBetweenDates
            (LocalDateTime fromDate,
             LocalDateTime toDate)
            throws BadRequestException;
}
