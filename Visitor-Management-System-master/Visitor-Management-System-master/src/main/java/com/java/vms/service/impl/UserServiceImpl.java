package com.java.vms.service.impl;

import com.java.vms.domain.Address;
import com.java.vms.domain.Flat;
import com.java.vms.domain.User;
import com.java.vms.model.Role;
import com.java.vms.model.UserDTO;
import com.java.vms.model.UserStatus;
import com.java.vms.repos.AddressRepository;
import com.java.vms.repos.FlatRepository;
import com.java.vms.repos.UserRepository;
import com.java.vms.service.UserService;
import com.java.vms.util.NotFoundException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

import com.java.vms.util.RedisCacheUtil;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final FlatRepository flatRepository;

    @Autowired
    private RedisCacheUtil redisCacheUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String USER_REDIS_KEY = "USR_";

    public UserServiceImpl(final UserRepository userRepository,
                           final AddressRepository addressRepository, final FlatRepository flatRepository) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.flatRepository = flatRepository;
    }

    public List<UserDTO> findAll() {
        final List<User> users = userRepository.findAll(Sort.by("id"));
        return users.stream()
                .map(user -> mapToDTO(user, new UserDTO()))
                .toList();
    }

    public UserDTO get(final Long id) {
        return userRepository.findById(id)
                .map(user -> mapToDTO(user, new UserDTO()))
                .orElseThrow(NotFoundException::new);
    }

    @Transactional
    public Long create
            (final @Valid UserDTO userDTO)
            throws SQLIntegrityConstraintViolationException
    {
        if(emailExists(userDTO.getEmail()) || phoneExists(userDTO.getPhone())){
            throw new SQLIntegrityConstraintViolationException("Email/Phone already exists");
        }
        final User user = new User();
        mapToEntity(userDTO, user);
        user.setUserStatus(UserStatus.ACTIVE);
        Long createdId = userRepository.save(user).getId();
        //Redis Caching USER*
        redisCacheUtil.setValueInRedisWithDefaultTTL(USER_REDIS_KEY + createdId, user);
        log.info("New user created with id: {}", createdId);
        return createdId;
    }

    @Transactional
    public void update(final UserDTO userDTO) {
        final User user = userRepository.findUserByEmail(userDTO.getEmail())
                .orElseThrow(() -> new NotFoundException("User not found for email: " + userDTO.getEmail()));
        mapToEntity(userDTO, user);
        log.info("User {} details updated.", user.getEmail());
        // Redis Caching USR_4
        redisCacheUtil.setValueInRedisWithDefaultTTL(USER_REDIS_KEY + user.getId(), user);
        userRepository.save(user);
    }

    public void markUserStatus(final Long id) throws NotFoundException {
        //Redis Caching USER_3
        User user = (User) redisCacheUtil.getValueFromRedisCache(USER_REDIS_KEY + id);
        if(user == null){
            user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found for ID: " + id));
        }
        if(user.getUserStatus() == UserStatus.ACTIVE){
            user.setUserStatus(UserStatus.INACTIVE);
        }
        else{
            user.setUserStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);
        redisCacheUtil.setValueInRedisWithDefaultTTL(USER_REDIS_KEY + id, user);
        log.info("User status updated for id: {}", user.getId());
    }

    private UserDTO mapToDTO(final User user, final UserDTO userDTO) {
        userDTO.setId(user.getId());
        userDTO.setName(user.getName());
        userDTO.setEmail(user.getEmail());
        userDTO.setPhone(user.getPhone());
        userDTO.setUserStatus(user.getUserStatus());
        userDTO.setRole(user.getRole());
        Address address = user.getAddress() == null ? null : user.getAddress();
        userDTO.setLine1(address.getLine1());
        userDTO.setLine2(address.getLine2());
        userDTO.setCity(address.getCity());
        userDTO.setState(address.getState());
        userDTO.setCountry(address.getCountry());
        userDTO.setPincode(address.getPincode());
        userDTO.setFlatNum(user.getFlat() == null ? null : user.getFlat().getFlatNum());
        return userDTO;
    }

    private void mapToEntity(final UserDTO userDTO, final User user) {
        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        user.setPhone(userDTO.getPhone());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
//      No need to set user status
        if(userDTO.getRole() != null) {
            user.setRole(userDTO.getRole());
        }

        //TODO: If user role is resident address should be given, if admin creating the user then address can be ignored.
        if(userDTO.getLine1() != null || userDTO.getLine2() != null ||
            userDTO.getCity() != null || userDTO.getState() != null ||
            userDTO.getCountry() != null || userDTO.getPincode() != null) {
            Address address = Address.builder().line1(userDTO.getLine1())
                    .line2(userDTO.getLine2())
                    .city(userDTO.getCity())
                    .state(userDTO.getState())
                    .country(userDTO.getCountry())
                    .pincode(userDTO.getPincode()).build();
            addressRepository.save(address);
            user.setAddress(address);
        }
        if(userDTO.getFlatNum() != null) {
            Flat flat = (Flat) redisCacheUtil.getValueFromRedisCache(userDTO.getFlatNum());
            if(flat == null){
                flat = userDTO.getFlatNum() == null ? null : flatRepository.findByFlatNum(userDTO.getFlatNum())
                        .orElseThrow(() -> new NotFoundException("flat not found for user: " + userDTO.getName()));
            }
            user.setFlat(flat);
            //TODO: Set flat status to NOT-AVAILABLE as it's assigned to a resident
        }
    }

    private boolean nameExists(final String name) {
        return userRepository.existsByNameIgnoreCase(name);
    }

    private boolean emailExists(final String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    private boolean phoneExists(final Long phone) {
        return userRepository.existsByPhone(phone);
    }

    public List<String> createUsersFromFile(MultipartFile file){
        log.info("Uploaded File: {}", file.getOriginalFilename());
        List<String> response = new ArrayList<>();
        try{
            BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream()));
            CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());
            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
            int counter = 0;
            for(CSVRecord record: csvRecords){
                counter++;
                if(record.get("name").isEmpty() || record.get("email").isEmpty()
                    || record.get("phone").isEmpty() || record.get("role").isEmpty()
                    || !record.get("phone").matches("^\\d.+$")
                    || !record.get("role").matches("^(ADMIN|GATEKEEPER|RESIDENT)$")){

                    response.add("Unable to create user: " + counter + " [Details provided are invalid]");
                    continue;
                }
                UserDTO userDTO = new UserDTO();
                try {
                    userDTO.setName(record.get("name"));
                    userDTO.setEmail(record.get("email"));
                    userDTO.setPhone(Long.valueOf(record.get("phone")));
                    userDTO.setRole(Role.valueOf(record.get("role")));
                    userDTO.setLine1(record.get("line1"));
                    userDTO.setLine2(record.get("line2"));
                    userDTO.setCity(record.get("city"));
                    userDTO.setState(record.get("state"));
                    userDTO.setCountry(record.get("country"));
                    userDTO.setPincode(Integer.valueOf(record.get("pincode")));
                    userDTO.setFlatNum(record.get("flatnum").isEmpty()?null:record.get("flatnum"));
                    long id = create(userDTO);
                    response.add("Created user " + userDTO.getName() + " with id: " + id);
                }
                catch (Exception e){
                    response.add("Unable to create user " + userDTO.getName() + " with err msg: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findUserByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User with " + username + " not found"));
    }
}
