package com.java.vms.service.impl;

import com.java.vms.domain.Flat;
import com.java.vms.model.FlatDTO;
import com.java.vms.model.FlatStatus;
import com.java.vms.repos.FlatRepository;
import com.java.vms.service.FlatService;
import com.java.vms.util.NotFoundException;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import com.java.vms.util.RedisCacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlatServiceImpl implements FlatService {

    private final FlatRepository flatRepository;

    private final RedisCacheUtil redisCacheUtil;

    public List<FlatDTO> findAll() {
        final List<Flat> flats = flatRepository.findAll(Sort.by("id"));
        return flats.stream()
                .map(flat -> mapToDTO(flat, new FlatDTO()))
                .toList();
    }

    public FlatDTO get(final Long id) {
        return flatRepository.findById(id)
                .map(flat -> mapToDTO(flat, new FlatDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final FlatDTO flatDTO) throws SQLIntegrityConstraintViolationException {
        if(flatNumExists(flatDTO.getFlatNum())){
            throw new SQLIntegrityConstraintViolationException("Flat with num: " + flatDTO.getFlatNum() + " already exists!");
        }
        final Flat flat = new Flat();
        mapToEntity(flatDTO, flat);
        flat.setFlatStatus(FlatStatus.AVAILABLE);
        log.info("FLAT created with num: {}", flatDTO.getFlatNum());
        //REDIS Caching for FLAT with flatNum
        Long createdFlatId = flatRepository.save(flat).getId();
        //template.opsForValue().set(flat.getFlatNum(), flat, 10, TimeUnit.MINUTES);
        redisCacheUtil.setValueInRedisWithDefaultTTL(flat.getFlatNum(), flat);
        return createdFlatId;
    }

    public Flat update(final FlatDTO flatDTO) throws NotFoundException {
        //TODO: Redis Caching FLAT~
        final Flat flat = flatRepository.findByFlatNum(flatDTO.getFlatNum())
                .orElseThrow(() -> new NotFoundException("Flat not found for num: " + flatDTO.getFlatNum()));
        mapToEntity(flatDTO, flat);
        flatRepository.save(flat);
        return flat;
    }

    private FlatDTO mapToDTO(final Flat flat, final FlatDTO flatDTO) {
//        flatDTO.setId(flat.getId());
        flatDTO.setFlatNum(flat.getFlatNum());
        flatDTO.setFlatStatus(flat.getFlatStatus());
        return flatDTO;
    }

    private void mapToEntity(final FlatDTO flatDTO, final Flat flat) {
        flat.setFlatNum(flatDTO.getFlatNum());
        if(flatDTO.getFlatStatus() != null){
            flat.setFlatStatus(flatDTO.getFlatStatus());
        }
        else{
            flat.setFlatStatus(FlatStatus.AVAILABLE);
        }
    }

    private boolean flatNumExists(final String flatNum) {
        return flatRepository.existsByFlatNumIgnoreCase(flatNum);
    }

    public FlatStatus changeFlatStatusToNotAvailable(String flatNum, boolean status) {
        // Check for FLAT in Redis Cache first, if not found, then hit DB.
        //Flat flat = (Flat) template.opsForValue().get(flatNum);
        Flat flat = (Flat) redisCacheUtil.getValueFromRedisCache(flatNum);
        if(flat == null){
            flat = flatRepository.findByFlatNum(flatNum)
                    .orElseThrow(() -> new NotFoundException("Flat not found for num: " + flatNum));
        }
        if(status)
            flat.setFlatStatus(FlatStatus.AVAILABLE);
        else
            flat.setFlatStatus(FlatStatus.NOTAVAILABLE);
        log.info("FLAT status changed for flat num: {} & LastUpdatedTimeStamp: {}", flatNum, flat.getLastUpdated());
        //update redis cache with updated flat object
        //template.opsForValue().set(flatNum, flat);
        redisCacheUtil.setValueInRedisWithDefaultTTL(flatNum, flat);
        return flatRepository.save(flat).getFlatStatus();
    }

}
