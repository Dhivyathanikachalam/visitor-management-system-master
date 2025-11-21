package com.java.vms.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreApproveDTO {

    @NotNull
    @Size(max = 255)
    private String name;

    @NotNull
    @Size(max = 255)
    private String phone;

    @NotNull
    @Size(max = 255)
    private String unqId;

    @NotNull
    private String line1;

    private String line2;

    @NotNull
    private String city;

    @NotNull
    private String state;

    @NotNull
    private String country;

    @NotNull
    private Integer pincode;

    @NotNull
    @Size(max = 255)
    private String visitorImgUrl;

    @NotNull
    @Size(max = 255)
    private String purpose;

    @NotNull
    private Integer numOfGuests;

}
