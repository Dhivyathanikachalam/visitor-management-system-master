package com.java.vms.config;

import com.java.vms.domain.Visit;
import com.java.vms.model.VisitStatus;
import com.java.vms.repos.VisitRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class ExpireVisitRequestAspect {

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private TaskExecutor taskExecutor;

    @Transactional
    @Around("@annotation(com.java.vms.config.ExpireVisitRequest)")
    public void handleExpiringVisitRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        //Before: No action before advice
        //actual
        Long visitId;
        try {
            visitId = (Long) joinPoint.proceed();
            Visit visit = visitRepository.findById(visitId).orElse(null);
            if (visit != null && visit.getVisitStatus() == VisitStatus.PREAPPROVED) {
                log.info("Visit status is PRE-APPROVED for visit id: {}, skipping expiration.", visitId);
                return;
            }
            else{
                log.info("Visit Request with id: {} will be expired in 10 minutes.", visitId);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        taskExecutor.execute(() -> {
            try {
                TimeUnit.MINUTES.sleep(10);
            } catch (Throwable e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            //after
            Visit delayedVisit = visitRepository.findById(visitId).orElse(null);
            if(delayedVisit != null && delayedVisit.getVisitStatus() == VisitStatus.PENDING){
                delayedVisit.setVisitStatus(VisitStatus.EXPIRED);
                log.info("Visit status EXPIRED for visit id: {}", visitId);
                visitRepository.save(delayedVisit);
            }
            else{
                 log.info("Visit status is not PENDING for visit id: {}", visitId);
            }
        });
    }
}
