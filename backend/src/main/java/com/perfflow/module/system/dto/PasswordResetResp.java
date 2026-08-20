package com.perfflow.module.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class PasswordResetResp implements Serializable {
    private String initialPassword;
}
