package com.perfflow.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改个人资料请求体。
 */
@Data
public class ProfileReq implements Serializable {

    @Size(max = 32)
    private String realName;

    @Email
    @Size(max = 64)
    private String email;

    @Size(max = 20)
    private String phone;
}
