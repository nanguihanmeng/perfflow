package com.perfflow.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码请求体。
 *
 * @author PerfFlow
 */
@Data
public class ChangePasswordReq implements Serializable {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 32, message = "新密码长度需在 8-32 位之间")
    private String newPassword;
}
