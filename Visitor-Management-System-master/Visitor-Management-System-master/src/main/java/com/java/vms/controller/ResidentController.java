package com.java.vms.controller;

import com.java.vms.model.PreApproveDTO;
import com.java.vms.model.Role;
import com.java.vms.model.UserDTO;
import com.java.vms.model.VisitDTO;
import com.java.vms.service.ResidentService;
import com.java.vms.service.UserService;
import com.java.vms.service.VisitService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.SneakyThrows;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

@RestController
@RequestMapping("/resident")
public class ResidentController {

    @Autowired
    private VisitService visitService;

    @Autowired
    private ResidentService residentService;

    @Autowired
    private UserService userService;

    @PutMapping("/approveReq/{id}")
    @ApiResponse(responseCode = "200")
    public ResponseEntity<Void> approveReq
            (@PathVariable(name = "id") @Valid Long visitId)
            throws BadRequestException
    {
        visitService.approveVisitReq(visitId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/rejectReq/{id}")
    @ApiResponse(responseCode = "200")
    public ResponseEntity<Void> rejectReq
            (@PathVariable(name = "id") @Valid Long visitId)
            throws BadRequestException
    {
        visitService.rejectVisitReq(visitId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/listAllVisitReqs")
    @ApiResponse(responseCode = "200")
    public ResponseEntity<List<VisitDTO>> getAllVisitRequestsByStatus
            (@RequestParam(name = "status") String status,
             @RequestParam(name = "name") String userName,
             @RequestParam(name = "phone") Long phone)
            throws BadRequestException
    {
        return ResponseEntity.ok().body(visitService.listAllVisitReqsByStatus(status, userName, phone, true));
    }

    @PostMapping("/preApproveVisitRequest")
    @ApiResponse(responseCode = "201")
    @SneakyThrows
    public ResponseEntity<Void> preApproveVisitRequest
            (@RequestBody @Valid PreApproveDTO preApproveDTO,
             @RequestParam(name = "userId") @Valid Long userId)
    {
        residentService.createPreApprovedVisitReq(preApproveDTO, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registerUser
            (@RequestBody @Valid UserDTO userDTO)
            throws SQLIntegrityConstraintViolationException
    {
        userDTO.setRole(Role.RESIDENT);
        Long userId = userService.create(userDTO);
        return ResponseEntity.ok().build();
    }
}
