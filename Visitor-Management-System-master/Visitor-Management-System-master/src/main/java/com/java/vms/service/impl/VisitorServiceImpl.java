package com.java.vms.service.impl;

import com.java.vms.domain.Address;
import com.java.vms.domain.Visitor;
import com.java.vms.model.PreApproveDTO;
import com.java.vms.model.VisitorDTO;
import com.java.vms.repos.AddressRepository;
import com.java.vms.repos.VisitorRepository;
import com.java.vms.service.VisitorService;
import com.java.vms.util.NotFoundException;
import java.util.List;

import com.java.vms.util.RedisCacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VisitorServiceImpl implements VisitorService {

    private final VisitorRepository visitorRepository;
    private final AddressRepository addressRepository;
    final String VISITOR_REDIS_KEY = "VISITOR_";

    @Autowired
    private RedisCacheUtil redisCacheUtil;

    public VisitorServiceImpl
            (final VisitorRepository visitorRepository,
            final AddressRepository addressRepository)
    {
        this.visitorRepository = visitorRepository;
        this.addressRepository = addressRepository;
    }

    public List<VisitorDTO> findAll() {
        final List<Visitor> visitors = visitorRepository.findAll(Sort.by("id"));
        return visitors.stream()
                .map(visitor -> mapToDTO(visitor, new VisitorDTO()))
                .toList();
    }

    public VisitorDTO get(final Long id) {
        return visitorRepository.findById(id)
                .map(visitor -> mapToDTO(visitor, new VisitorDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final VisitorDTO visitorDTO) {
        final Visitor visitor = new Visitor();
        if(unqIdExists(visitorDTO.getUnqId())){
            log.info("Visitor already exists with unq ID: {}", visitorDTO.getUnqId());
            return visitorRepository.findVisitorByUnqId(visitorDTO.getUnqId()).get().getId();
        }
        mapToEntity(visitorDTO, visitor);
        log.info("Visitor created with unq id: {}", visitorDTO.getUnqId());
        Long savedVisitorId = visitorRepository.save(visitor).getId();
        //Redis Cache VISITOR*
        redisCacheUtil.setValueInRedisWithDefaultTTL(VISITOR_REDIS_KEY + savedVisitorId, visitor);
        return savedVisitorId;
    }

    public Long create(final PreApproveDTO preApproveDTO) {
        VisitorDTO visitorDTO = new VisitorDTO();
        mapPreApprovedDTOToVisitorDTO(preApproveDTO, visitorDTO);
        return create(visitorDTO);
    }

    public void update(final Long id, final VisitorDTO visitorDTO) {
        final Visitor visitor = visitorRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(visitorDTO, visitor);
        visitorRepository.save(visitor);
    }

    public void delete(final Long id) {
        visitorRepository.deleteById(id);
    }

    private VisitorDTO mapToDTO(final Visitor visitor, final VisitorDTO visitorDTO) {
        visitorDTO.setId(visitor.getId());
        visitorDTO.setName(visitor.getName());
        visitorDTO.setPhone(visitor.getPhone());
        visitorDTO.setUnqId(visitor.getUnqId());
        Address address = visitor.getAddress() == null ? null : visitor.getAddress();
        visitorDTO.setLine1(address.getLine1());
        visitorDTO.setLine2(address.getLine2());
        visitorDTO.setCity(address.getCity());
        visitorDTO.setState(address.getState());
        visitorDTO.setCountry(address.getCountry());
        visitorDTO.setPincode(address.getPincode());
        //visitorDTO.setAddress(visitor.getAddress() == null ? null : visitor.getAddress().getId());
        return visitorDTO;
    }

    private void mapToEntity(final VisitorDTO visitorDTO, final Visitor visitor) {
        visitor.setName(visitorDTO.getName());
        visitor.setPhone(visitorDTO.getPhone());
        visitor.setUnqId(visitorDTO.getUnqId());
        if(visitorDTO.getLine1() != null || visitorDTO.getLine2() != null ||
                visitorDTO.getCity() != null || visitorDTO.getState() != null ||
                visitorDTO.getCountry() != null || visitorDTO.getPincode() != null) {
                final Address address = Address.builder().line1(visitorDTO.getLine1())
                    .line2(visitorDTO.getLine2())
                    .city(visitorDTO.getCity())
                    .state(visitorDTO.getState())
                    .country(visitorDTO.getCountry())
                    .pincode(visitorDTO.getPincode()).build();
            addressRepository.save(address);
            visitor.setAddress(address);
        }
    }

    private void mapPreApprovedDTOToVisitorDTO(final PreApproveDTO preApproveDTO, VisitorDTO visitorDTO){
        visitorDTO.setName(preApproveDTO.getName());
        visitorDTO.setPhone(preApproveDTO.getPhone());
        visitorDTO.setUnqId(preApproveDTO.getUnqId());
        visitorDTO.setLine1(preApproveDTO.getLine1());
        visitorDTO.setLine2(preApproveDTO.getLine2());
        visitorDTO.setCity(preApproveDTO.getCity());
        visitorDTO.setState(preApproveDTO.getState());
        visitorDTO.setCountry(preApproveDTO.getCountry());
        visitorDTO.setPincode(preApproveDTO.getPincode());
    }

    private boolean phoneExists(final String phone) {
        return visitorRepository.existsByPhoneIgnoreCase(phone);
    }

    public boolean unqIdExists(final String unqId) {
        return visitorRepository.existsByUnqIdIgnoreCase(unqId);
    }

}
