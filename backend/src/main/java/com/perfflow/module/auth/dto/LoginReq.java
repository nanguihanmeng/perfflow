package com.perfflow.module.auth.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;
@Data
public class LoginReq implements Serializable {

    @NotBlank
    // 登录名。
    private String username;
    @NotBlank
    // 密码。
    private String password;
}
