package com.java.vms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class VisitDTO {

    @JsonIgnore
    private Long id;

    @NotNull
    private Long visitor;

    //@NotNull
    private VisitStatus visitStatus;

    private LocalDateTime inTime;

    //@NotNull
    private LocalDateTime outTime;

    @NotNull
    @Size(max = 255)
    private String visitorImgUrl;

    @NotNull
    @Size(max = 255)
    private String purpose;

    @NotNull
    private Integer numOfGuests;

    @NotNull
    private String userName;

    @NotNull
    private Long userPhoneNumber;

    @NotNull
    private String flatNum;

}
