package com.perfflow.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class LoginReq implements Serializable {

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
