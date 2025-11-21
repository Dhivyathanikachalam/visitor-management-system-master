package com.java.vms.service.impl;

import com.java.vms.config.ExpireVisitRequest;
import com.java.vms.domain.Flat;
import com.java.vms.domain.User;
import com.java.vms.domain.Visit;
import com.java.vms.domain.Visitor;
import com.java.vms.model.PreApproveDTO;
import com.java.vms.model.VisitDTO;
import com.java.vms.model.VisitStatus;
import com.java.vms.repos.FlatRepository;
import com.java.vms.repos.UserRepository;
import com.java.vms.repos.VisitRepository;
import com.java.vms.repos.VisitorRepository;
import com.java.vms.service.VisitService;
import com.java.vms.util.NotFoundException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.java.vms.util.RedisCacheUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class VisitServiceImpl implements VisitService {

    private final VisitRepository visitRepository;
    private final UserRepository userRepository;
    private final FlatRepository flatRepository;
    private final VisitorRepository visitorRepository;

    private final String VISITOR_REDIS_KEY = "VISITOR_";
    private final String USER_REDIS_KEY = "USR_";
    private final String VISIT_REDIS_KEY = "VISIT_";

    @Autowired
    private RedisCacheUtil redisCacheUtil;

    public VisitServiceImpl
            (final VisitRepository visitRepository,
             final UserRepository userRepository,
             final FlatRepository flatRepository,
             final VisitorRepository visitorRepository)
    {
        this.visitRepository = visitRepository;
        this.userRepository = userRepository;
        this.flatRepository = flatRepository;
        this.visitorRepository = visitorRepository;
    }

    public List<VisitDTO> findAll(Pageable pageable) {
        if(pageable == null){
            return null;
        }
        final List<Visit> visits = visitRepository.findAll(pageable).stream().toList();
        return visits.stream()
                .map(visit -> mapToDTO(visit, new VisitDTO()))
                .toList();
    }

    public VisitDTO get(final Long id) {
        return visitRepository.findById(id)
                .map(visit -> mapToDTO(visit, new VisitDTO()))
                .orElseThrow(NotFoundException::new);
    }

    @ExpireVisitRequest
    @Transactional
    public Long create(final VisitDTO visitDTO) throws BadRequestException {
        Visit visit = new Visit();
        mapToEntity(visitDTO, visit);
        log.info("Visit created for visitor with Id: {}", visitDTO.getVisitor());
        Long visitId = visitRepository.save(visit).getId();
        //Redis Caching VISIT*
        redisCacheUtil.setValueInRedisWithDefaultTTL(VISIT_REDIS_KEY + visitId, visit);
        return visitId;
    }

    /* Dead Code
    @Transactional
    public Long create
            (final PreApproveDTO preApproveDTO,
             final Long visitorId,
             final Long userId)
            throws BadRequestException
    {
        Visit visit = new Visit();
        mapPreApprovedDTOToEntity(preApproveDTO, visit, visitorId, userId);
        log.info("Visit created for visitor with Id: " + visitorId);
        Long preApprovedVisitId = visitRepository.save(visit).getId();
        //Redis Caching PreApproved-VISIT*
        redisCacheUtil.setValueInRedisWithDefaultTTL(VISIT_REDIS_KEY + preApprovedVisitId, visit);
        return preApprovedVisitId;
    }
    */

    public void update(final Long id, final VisitDTO visitDTO) throws BadRequestException {
        final Visit visit = visitRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(visitDTO, visit);
        visitRepository.save(visit);
    }

    private VisitDTO mapToDTO(final Visit visit, final VisitDTO visitDTO) {
        visitDTO.setId(visit.getId());
        visitDTO.setVisitStatus(visit.getVisitStatus());
        visitDTO.setInTime(visit.getInTime());
        visitDTO.setOutTime(visit.getOutTime());
        visitDTO.setVisitorImgUrl(visit.getVisitorImgUrl());
        visitDTO.setPurpose(visit.getPurpose());
        visitDTO.setNumOfGuests(visit.getNumOfGuests());
        visitDTO.setUserName(visit.getUser() == null ? null : visit.getUser().getName());
        visitDTO.setUserPhoneNumber(visit.getUser() == null ? null : visit.getUser().getPhone());
        visitDTO.setFlatNum(visit.getFlat() == null ? null : visit.getFlat().getFlatNum());
        visitDTO.setVisitor(visit.getVisitor() == null ? null : visit.getVisitor().getId());
        return visitDTO;
    }

    private void mapToEntity(final VisitDTO visitDTO, final Visit visit) throws BadRequestException {
        boolean isVisitRequestPreApproved = visitDTO.getVisitStatus() == VisitStatus.PREAPPROVED;
        visit.setVisitStatus(isVisitRequestPreApproved ? VisitStatus.PREAPPROVED: VisitStatus.PENDING);
        //visit.setInTime(visitDTO.getInTime());
        //visit.setOutTime(visitDTO.getOutTime());
        visit.setVisitorImgUrl(visitDTO.getVisitorImgUrl());
        visit.setPurpose(visitDTO.getPurpose());
        visit.setNumOfGuests(visitDTO.getNumOfGuests());
        //TODO: Need to optimize hitting the DB and modify caching accordingly.
        final User user = visitDTO.getUserName() == null && visitDTO.getUserPhoneNumber() == null ?
                null : userRepository.findUserByNameAndPhone(visitDTO.getUserName(), visitDTO.getUserPhoneNumber())
                .orElseThrow(() -> new NotFoundException("user not found for resident: " + visitDTO.getUserName()));
        Flat flat;
        if(isVisitRequestPreApproved){
            flat = user.getFlat();
            if(flat == null){
                throw new NotFoundException("Flat not found for user");
            }
        }
        else{
            //Redis Caching FLAT_1
            flat = (Flat) redisCacheUtil.getValueFromRedisCache(visitDTO.getFlatNum());
            if(flat == null){
                flat = visitDTO.getFlatNum() == null ? null : flatRepository.findByFlatNum(visitDTO.getFlatNum())
                        .orElseThrow(() -> new NotFoundException("flat not found for flat num: " + visitDTO.getFlatNum()));
            }
        }
        //Redis Caching VISITOR_1
        Visitor visitor = (Visitor) redisCacheUtil.getValueFromRedisCache(VISITOR_REDIS_KEY + visitDTO.getVisitor());
        if(visitor == null){
            visitor = visitDTO.getVisitor() == null ? null : visitorRepository.findById(visitDTO.getVisitor())
                    .orElseThrow(() -> new NotFoundException("visitor not found for id: " + visitDTO.getVisitor()));
        }
        if (user == null || flat == null || visitor == null){
            throw new BadRequestException("Invalid request received. Please provide valid details.");
        }
        visit.setFlat(flat);
        visit.setUser(user);
        visit.setVisitor(visitor);
    }

    public void mapPreApprovedDTOToEntity
            (final PreApproveDTO preApproveDTO,
             Visit visit,
             Long visitorID,
             Long userId)
    {
        //Redis Caching VISITOR_2
        Visitor visitor = (Visitor) redisCacheUtil.getValueFromRedisCache(VISITOR_REDIS_KEY + visitorID);
        if(visitor == null){
            visitor = visitorRepository.findById(visitorID)
                    .orElseThrow(() -> new NotFoundException("visitor not found for id: " + visitorID));
        }
        //Redis Caching USER_1
        User user = (User) redisCacheUtil.getValueFromRedisCache(USER_REDIS_KEY + userId);
        if(user == null){
            user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("user not found with id: " + userId));
        }
        visit.setVisitStatus(VisitStatus.APPROVED);
        visit.setVisitorImgUrl(preApproveDTO.getVisitorImgUrl());
        visit.setPurpose(preApproveDTO.getPurpose());
        visit.setNumOfGuests(preApproveDTO.getNumOfGuests());
        visit.setVisitor(visitor);
        visit.setUser(user);
        visit.setFlat(user.getFlat());
    }

    public Long anyPreApprovedExists(Long visitorId, Long userId) throws BadRequestException {
        //Redis Caching VISITOR_3
        Visitor visitor = (Visitor) redisCacheUtil.getValueFromRedisCache(VISITOR_REDIS_KEY + visitorId);
        if(visitor == null){
            visitor = visitorRepository.findById(visitorId).orElse(null);
        }
        //Redis Caching USER_1
        User user = (User) redisCacheUtil.getValueFromRedisCache(USER_REDIS_KEY + userId);
        if(user == null){
            user = userRepository.findById(userId).orElse(null);
        }
        if(visitor == null || user == null){
            throw new BadRequestException("Invalid visitorId or userID");
        }
        Visit preApprovedVisit = visitRepository.isPreApprovedExistsForVisitor(VisitStatus.PREAPPROVED, visitor, user)
                .orElseThrow(() -> new NotFoundException("Pre-approved visit request not found for visitor with id: " + visitorId));

        return preApprovedVisit.getId();
    }

    public void markVisitorEntry(Long visitId) throws BadRequestException {
        //Redis Caching VISIT_1
        Visit visit = (Visit) redisCacheUtil.getValueFromRedisCache(VISIT_REDIS_KEY + visitId);
        if(visit == null){
            visit = visitRepository.findById(visitId)
                    .orElseThrow(() -> new BadRequestException("No visit request found for id: " + visitId));
        }
        //TODO: Need to add check for inTime
        if(visit.getVisitStatus() == VisitStatus.PREAPPROVED || visit.getVisitStatus() == VisitStatus.APPROVED){
            visit.setInTime(LocalDateTime.now());
            log.info("Marked entry for visitor with id: {}", visit.getVisitor().getId());
            visitRepository.save(visit);
        }
        else if(visit.getVisitStatus() == VisitStatus.REJECTED){
            log.warn("Visit request rejected for id: {}", visit.getId());
        }
        else{
            throw new BadRequestException("Visit request not yet approved.");
        }
        //Cache the updated visit object
        redisCacheUtil.setValueInRedisWithDefaultTTL(VISIT_REDIS_KEY + visitId, visit);
    }

    public void markVisitorExit(Long visitId) throws BadRequestException {
        //Redis Caching VISIT_2
        Visit visit = (Visit) redisCacheUtil.getValueFromRedisCache(VISIT_REDIS_KEY + visitId);
        if(visit == null){
            visit = visitRepository.findById(visitId)
                    .orElseThrow(() -> new BadRequestException("No visit request found for id: " + visitId));
        }
        if(visit.getInTime() != null && visit.getOutTime() == null){
            visit.setOutTime(LocalDateTime.now());
            visit.setVisitStatus(VisitStatus.COMPLETED);
            log.info("Marked exit for visitor with id: {}", visit.getVisitor().getId());
            visitRepository.save(visit);
        }
        else{
            throw new BadRequestException("Visitor entry not found.");
        }
        //Cache the updated visit object
        redisCacheUtil.setValueInRedisWithDefaultTTL(VISIT_REDIS_KEY + visitId, visit);
    }

    public void approveVisitReq(Long visitId) throws BadRequestException {
        //Redis Caching VISIT_3
        Visit visit = (Visit) redisCacheUtil.getValueFromRedisCache(VISIT_REDIS_KEY + visitId);
        if(visit == null){
            visit = visitRepository.findById(visitId)
                    .orElseThrow(() -> new BadRequestException("No visit request found for id: " + visitId));
        }
        if(visit.getVisitStatus() != VisitStatus.PENDING){
            log.info("Visit req with id {} is not pending", visit.getId());
            throw new BadRequestException("Visit req with id " + visit.getId() + " is not pending");
        }
        visit.setVisitStatus(VisitStatus.APPROVED);
        log.info("Approved visit request with id: {}", visit.getId());
        //Cache the updated visit object
        redisCacheUtil.setValueInRedisWithDefaultTTL(VISIT_REDIS_KEY + visitId, visit);
        visitRepository.save(visit);
    }

    public void rejectVisitReq(Long visitId) throws BadRequestException {
        //Redis Caching VISIT_4
        Visit visit = (Visit) redisCacheUtil.getValueFromRedisCache(VISIT_REDIS_KEY + visitId);
        if(visit == null){
            visit = visitRepository.findById(visitId)
                    .orElseThrow(() -> new BadRequestException("No visit request found for id: " + visitId));
        }
        if(visit.getVisitStatus() != VisitStatus.PENDING){
            log.info("Visit req with id {} is not pending", visit.getId());
            throw new BadRequestException("Visit req with id " + visit.getId() + " is not pending");
        }
        visit.setVisitStatus(VisitStatus.REJECTED);
        log.info("Rejected visit request with id: {}", visit.getId());
        //Cache the updated visit object
        redisCacheUtil.setValueInRedisWithDefaultTTL(VISIT_REDIS_KEY + visitId, visit);
        visitRepository.save(visit);
    }

    public List<VisitDTO> listAllVisitReqsByStatus
            (String status,
             String userName,
             Long userPhone,
             boolean isDurationEnabled)
            throws BadRequestException
    {
        final User user = userRepository.findUserByNameAndPhone(userName, userPhone)
                .orElseThrow(() -> new BadRequestException("User not found  with given details."));
        VisitStatus vStatus = VisitStatus.APPROVED.name().equalsIgnoreCase(status)? VisitStatus.APPROVED : VisitStatus.REJECTED;
        List<Visit> visits = visitRepository.findVisitByVisitStatusAndUser(vStatus, user);
        if (visits.isEmpty()){
            throw new NotFoundException("No visit requests found with status: " + status);
        }
        List<VisitDTO> visitDTOs = new ArrayList<>();
        VisitDTO visitDTO;
        for(Visit visit: visits){
            if(isDurationEnabled){
                Duration duration = Duration.between(LocalDateTime.now(), visit.getDateCreated()).abs();
                if(duration.toDays() > 30L){
                    log.info("Ignored visit request with id: {}", visit.getId());
                    continue;
                }
            }
            visitDTO = new VisitDTO();
            visitDTOs.add(mapToDTO(visit, visitDTO));
        }
        log.info("List of visits [{}] found for user: {} with status: {}", visitDTOs.size(), userName, status);
        return visitDTOs;
    }

    public String uploadVisitorImage(MultipartFile file){
        String fileName = UUID.randomUUID()  + "_" + file.getOriginalFilename();
        // Used to get user home directory in any OS
        String userHome = System.getProperty("user.home");
        // Joins visitorImage dir with user home path
        Path uploadDirPath = Paths.get(userHome, "visitor_imgs");
        try{
            // Ensure that the directory is created for us to store the images.
            Files.createDirectories(uploadDirPath);
        }catch (Exception e){
            throw new RuntimeException("Failed to create directory for visitor images");
        }
        // Adds generated file name to uploaded directory path.
        Path uploadFilePath = uploadDirPath.resolve(fileName);
        String response = "/content/" + fileName;
        try{
            // Transfer the uploaded file to the target location
            file.transferTo(uploadFilePath.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to store visitor image");
        }
        log.info("visitor-image uploaded successfully");
        return response;
    }

    public byte[] getAllVisitRequestsBetweenDates
            (LocalDateTime fromDate,
             LocalDateTime toDate)
            throws BadRequestException
    {
        if(fromDate.isAfter(toDate)){
            log.info("Invalid from and to dates: [{}, {}]", fromDate.toLocalDate(), toDate.toLocalDate());
            throw new BadRequestException("Invalid from and to dates.");
        }
        List<Visit> visits = visitRepository.findVisitsBetweenDates(OffsetDateTime.of(fromDate, ZoneOffset.UTC),
                        OffsetDateTime.of(toDate, ZoneOffset.UTC)).get();
        if(visits.isEmpty()){
            throw new NotFoundException("No visit requests found between " +
                    fromDate.toLocalDate() + " and " + toDate.toLocalDate());
        }
        List<VisitDTO> visitDTOS = new ArrayList<>();
        byte[] response;
        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.withHeader("Visitor", "VisitStatus", "In Time",
                "Out Time", "VisitorImgUrl", "Purpose", "Num of Guests", "User Name", "User Phone", "Flat Num"))){

            for(Visit visit: visits){
                VisitDTO visitDTO = new VisitDTO();
                mapToDTO(visit,visitDTO);
                visitDTOS.add(visitDTO);
                printer.printRecord(visitDTO.getVisitor(), visitDTO.getVisitStatus(),
                        visitDTO.getInTime(), visitDTO.getOutTime(), visitDTO.getVisitorImgUrl(),
                        visitDTO.getPurpose(), visitDTO.getNumOfGuests(), visitDTO.getUserName(),
                        visitDTO.getUserPhoneNumber(), visitDTO.getFlatNum());

            }

            printer.flush();
            response = byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("File: VisitReport_{}_{}.csv generated successfully.", fromDate.toLocalDate(), toDate.toLocalDate());
        return response;
    }

}
