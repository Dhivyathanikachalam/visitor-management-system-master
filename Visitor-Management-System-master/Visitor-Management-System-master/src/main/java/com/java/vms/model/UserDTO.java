package com.java.vms.model;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

//TODO: SRP for both UserDTO and PreApproveDTO
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;

    @NotNull
    @Size(max = 255)
    private String name;

    @NotNull
    @Size(max = 255)
    private String email;

    @NotNull
    private Long phone;

    @NotNull
    @Size(max = 18)
    private String password;

    private UserStatus userStatus;

    private Role role;

    private String line1;

    private String line2;

    private String city;

    private String state;

    private String country;

    private Integer pincode;

    private String flatNum;

}
