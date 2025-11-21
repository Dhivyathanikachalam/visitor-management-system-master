package com.java.vms.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlatDTO {

    @NotNull
    @Size(max = 255)
    private String flatNum;

    private FlatStatus flatStatus;

}
