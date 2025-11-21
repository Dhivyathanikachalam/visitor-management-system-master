package com.java.vms.service.impl;

import com.java.vms.domain.User;
import com.java.vms.model.PreApproveDTO;
import com.java.vms.model.VisitDTO;
import com.java.vms.model.VisitStatus;
import com.java.vms.model.VisitorDTO;
import com.java.vms.repos.FlatRepository;
import com.java.vms.repos.UserRepository;
import com.java.vms.repos.VisitRepository;
import com.java.vms.repos.VisitorRepository;
import com.java.vms.service.ResidentService;
import com.java.vms.util.NotFoundException;
import com.java.vms.util.RedisCacheUtil;
import jakarta.transaction.Transactional;
import lombok.SneakyThrows;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ResidentServiceImpl implements ResidentService {

    final VisitRepository visitRepository;
    final UserRepository userRepository;
    final FlatRepository flatRepository;
    final VisitorRepository visitorRepository;
    final VisitServiceImpl visitService;
    final VisitorServiceImpl visitorService;
    final String USER_REDIS_KEY = "USR_";

    @Autowired
    RedisCacheUtil redisCacheUtil;

    public ResidentServiceImpl
            (final VisitRepository visitRepository,
             final UserRepository userRepository,
             final FlatRepository flatRepository,
             final VisitorRepository visitorRepository,
             final VisitServiceImpl visitService,
             final VisitorServiceImpl visitorService)
    {
        this.visitRepository = visitRepository;
        this.userRepository = userRepository;
        this.flatRepository = flatRepository;
        this.visitorRepository = visitorRepository;
        this.visitService = visitService;
        this.visitorService = visitorService;
    }

    @SneakyThrows
    @Transactional
    public void createPreApprovedVisitReq
            (final PreApproveDTO preApproveDTO,
             final Long userId)
    {

        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        /* VISITOR model mapping from pre-approved DTO */
        Long visitorId = visitorService.create(modelMapper.map(preApproveDTO, VisitorDTO.class));
        log.info("Visitor created with id: {}", visitorId);

        /* VISIT model mapping from pre-approved DTO*/
        VisitDTO preApprovedVisitDTO = modelMapper.map(preApproveDTO, VisitDTO.class);
        preApprovedVisitDTO.setVisitor(visitorId);
        //Hit redis for user object
        User user = (User) redisCacheUtil.getValueFromRedisCache(USER_REDIS_KEY+userId);
        if(user == null){
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found for ID: " + userId));
            redisCacheUtil.setValueInRedisWithDefaultTTL(USER_REDIS_KEY + userId, user);
        }
        preApprovedVisitDTO.setUserName(user.getName());
        preApprovedVisitDTO.setUserPhoneNumber(user.getPhone());

        //Set visit-request status to pre-approved
        preApprovedVisitDTO.setVisitStatus(VisitStatus.PREAPPROVED);
        Long visitId = visitService.create(preApprovedVisitDTO);
        //TODO: Handle errors from visitService

        //Long visitId = visitService.create(preApproveDTO, visitorId, userId);
        log.info("Pre-approved visit request created for visitor id: {}", visitorId);
    }

}
